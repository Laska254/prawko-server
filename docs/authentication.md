# Authentication

The API is stateless - every request that requires authentication must carry an access token as
`Authorization: Bearer ...`. HTTP Basic isn't supported - such a request counts as unauthenticated (`401`).

## Tokens

Signing in with `POST /auth` (`{"userName": "...", "password": "..."}`, username or email, both ignoring case, the username on MariaDB also [accents](api.md#endpoints)) returns a token pair:

```json
{"accessToken": "...", "refreshToken": "...", "expiresIn": 900}
```

The access token is a signed JWT, valid for `access-token.validity` (15 minutes, `expiresIn` in seconds). It carries
the user's role, so a role change or account deletion applies only once it expires. An invalid or expired token is
answered with `401` and `WWW-Authenticate: Bearer error="invalid_token"`.

Once it expires, `POST /auth/refresh` with `{"refreshToken": "..."}` returns a new pair with the user's current role.
Access tokens sent to `POST /auth/**` are ignored, so a client may keep attaching its expired one when refreshing.
A refresh token is valid for `refresh-token.validity` (30 days) and can be used only once - each refresh returns a new
one, valid for the full period again. An unknown, expired or already used refresh token is answered with `401`, and
using one again also revokes every other refresh token of the user, signing out all their clients - a used token
showing up again means it was copied.

The exception is a retry after a lost response: within `refresh-token.reuse-grace` (30 seconds) of its first use, a
used token whose successor was never used returns a new pair. The successor it got before is revoked, so a token has
at most one live successor, and using that earlier successor counts as reuse. If a copied token is used within the
grace period, the legitimate client's token stops working, and its next refresh revokes all tokens, the copy's
included. A token revoked because of reuse can't be retried either. Two concurrent refreshes with one token can still
end as reuse, so clients must not refresh concurrently. Expired refresh tokens are deleted every
`refresh-token.cleanup-interval`.

Signing out with `POST /auth/logout` (`{"refreshToken": "..."}`) revokes the refresh token and answers `204`, also for
an unknown one. The client's access token stays valid until it expires, so the client should discard it too.
Changing (`PATCH /users/me/password`) or resetting (`POST /auth/password/reset`) the password revokes all refresh tokens
of the user, signing out all their clients - including the one that changed it, which has to sign in again.

## Google sign-in

Signing in with Google: the client gets an ID token from Google (Credential Manager on Android, Google Identity
Services on the web) and sends it to `POST /auth/google` as `{"idToken": "..."}`, which returns the same token pair as
`POST /auth`. Every client requests the token for the web (server) OAuth client ID, created in the Google Cloud console;
on Android it's passed to `GetGoogleIdOption.Builder.setServerClientId`, not the Android client ID. The server checks
the token's signature against Google's keys, its expiry, the issuer and that its audience is one of
`google.client-ids`. An invalid token is answered with `401`, without configured client IDs the endpoint answers `404`,
and if Google's keys can't be fetched `503`. The Google account is linked to a user on its first sign-in, looked up by
its email, which Google must have verified (otherwise `403`): an existing user gets it linked (`409` if it's linked to
another Google account already), an unverified one is verified and its password cleared (it may have been registered
by someone else with that email), and otherwise a new user without a password is created, with a username from the
email's local part. An existing user is only linked if Google hosts the email (Gmail or Google Workspace), otherwise
`409`: any other address was verified by Google only once and may belong to someone else by now. Later sign-ins find
the user by the Google account alone, so changing the email on either side keeps them linked.

## GitHub sign-in

Signing in with GitHub: the client runs GitHub's authorization code flow with PKCE and the scope `user:email`, using the
client ID of the GitHub OAuth app, and sends the result to `POST /auth/github` as
`{"code": "...", "codeVerifier": "...", "redirectUri": "..."}`, which returns the same token pair as `POST /auth`. The
server exchanges the code with the app's client secret and reads the account's ID and primary email from the GitHub API;
the GitHub access token is used only for that and isn't stored. A code GitHub rejects is answered with `401`, without a
configured client ID and secret the endpoint answers `404`, and if GitHub can't be reached, answers with a server error
or rate limits the app `503`. The first sign-in creates a user without a password, with the GitHub login as username.
The primary email must be verified by GitHub (otherwise `403`). If a user with that email exists the answer is `409` and
nothing is linked: GitHub doesn't host email addresses, it verified the address only once and it may belong to someone
else by now. Later sign-ins find the user by the GitHub account's ID alone, so renaming the login or changing the email
keeps them linked.

## Linking accounts

Linking and unlinking: a signed-in user links a provider account with `POST /users/me/identities/google` or
`POST /users/me/identities/github`, with the body of the matching `/auth` endpoint plus `currentPassword` and the same
errors for a bad credential, and `204` on success. The account's email doesn't have to match the user's, so this is the
way forward after the `409` above: sign in as that user, then link. The answer is `409` if the account is linked to
another user or the user already has an account of that provider. `DELETE /users/me/identities/google` or
`DELETE /users/me/identities/github` with `{"currentPassword": "..."}` unlinks it and answers `204`, `404` if it isn't
linked, and `409` if they have no password and no other linked account, as that would leave no way to sign in. Requiring
the password means a stolen access token alone can't add a lasting way in or remove the user's own: if it's missing or
wrong the answer is `400` `"Current password is incorrect."`, nothing changes and a GitHub code isn't used up. A user
without a password leaves it out, and can send no body when unlinking. Every link and unlink through these endpoints
emails a notice to the user's current address, so users without a password get warned too; if it can't be queued the
answer is `503` and nothing changes. A link made by a first sign-in sends no such notice: the provider hosts the email,
so it would go to the mailbox of the account that just signed in. If the user already has other accounts linked, one of
them may belong to someone who held the email earlier, so that mailbox gets a notice naming them (`503` and no link if
it can't be queued), and the password reset email names the linked accounts too. Removing one is left to the user.
`providers` in the user response lists the linked ones.
An unlinked account no longer finds the user by its ID, so its next sign-in counts as a first one: if the user's email
is one the provider hosts it's linked again, since whoever holds that account controls the mailbox and could reset the
password anyway. Unlinking doesn't revoke refresh tokens, so clients already signed in stay signed in.

In `prod` the proxy limits `POST /auth/google`, `POST /auth/github` and linking and unlinking per client IP
([Rate limiting](deployment.md#rate-limiting)). Over the limit they answer `429` with `Retry-After` (seconds), and
nothing is signed in, linked or unlinked.

## Users without a password

Users without a password: a user created by a provider has none, and `hasPassword` in the user response is `false`, so
a client can offer to set one. Until then `POST /auth` answers `401`, and `PATCH /users/me/password` and
`DELETE /users/me` answer `400` `"Current password is incorrect."`. `POST /auth/password/forgot` and
`POST /auth/password/reset` work without a current password and set the first one, after which all three work as usual.

## Roles

There are two roles: `USER` (every registered user) and `ADMIN`. `ADMIN` includes all `USER` permissions.

## Email verification

A new account must verify its email address before signing in. Registration emails a link to the frontend page
(`email-verification.url`) with a `token` query parameter, which the page sends to `POST /auth/verify` as
`{"token": "..."}`. Until then `POST /auth` answers `403` (only with the correct password, otherwise `401`), so the
account gets no tokens. Another link can be requested with `POST /auth/verify/resend`. Accounts not
verified before their link expires are deleted, so the username and email can be registered again.

## Changing the email

Changing the email with `PATCH /users/{id}` works the same way: the new address is only stored as pending and a link is
emailed to it (`pendingEmail` in the user response), and a confirmation link to the current address, so a stolen
password alone can't move the account to another inbox. Only the user themselves may change their email - admins get
`403`, so a stolen admin account can't take over other accounts this way either. Users changing their own email or
username also send `currentPassword`, so a stolen access token alone can't start a change or send links to the user's
inbox: if it's missing or wrong the answer is `400` `"Current password is incorrect."` and nothing changes. Users
without a password leave it out, and cancelling a change doesn't need it. Both links go to `POST /auth/verify`, in any
order; the user response tells which are still unopened (`awaitingNewEmailConfirmation`,
`awaitingCurrentEmailConfirmation`). The current email stays in use (sign-in, password reset) until both links are used.
Changing or resetting the password cancels a pending change, and so does sending the current email in the `PATCH`. For
new links send the `PATCH` again after the cooldown - within it the `PATCH` answers `429` and changes nothing.

## Deleting an account

Deleting an account (`DELETE /users/me`, or `DELETE /users/{id}` by an admin) removes its exams and refresh tokens and
invalidates any pending reset or verification links; the username and email can be registered again.
