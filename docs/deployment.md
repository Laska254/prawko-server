# Deployment

## Environment variables

The `prod` profile (`application-prod.yaml`: MariaDB, schema updated by Hibernate, Swagger disabled) holds no
credentials - all connection details come from environment variables:

* `DB_URL` e.g. `jdbc:mariadb://localhost:3306/prawko`
* `DB_USERNAME`
* `DB_PASSWORD`
* `CORS_ALLOWED_ORIGIN_PATTERNS` comma-separated, e.g. `https://prawko.pl`
* `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` SMTP server (with auth and STARTTLS)
* `MAIL_FROM` sender address, e.g. `no-reply@prawko.pl`
* `ACCESS_TOKEN_SECRET` Base64-encoded key signing access tokens, at least 256 bits, e.g. from
  `openssl rand -base64 32`
* `PASSWORD_RESET_URL` frontend page the reset link points to, e.g. `https://prawko.pl/auth/password/reset`
* `EMAIL_VERIFICATION_URL` frontend page the email verification link points to, e.g. `https://prawko.pl/auth/verify`
* `GOOGLE_CLIENT_IDS` optional, comma-separated web (server) OAuth client IDs the apps request Google ID tokens
  for ([Google sign-in](authentication.md#google-sign-in)); without it `POST /auth/google` answers `404`
* `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` optional, credentials of the GitHub OAuth app
  ([GitHub sign-in](authentication.md#github-sign-in)); without them `POST /auth/github` answers `404`
* `MEDIA_DIRECTORY` directory with question media files, e.g. `/media` (a Docker volume)
* `MEDIA_BASE_URL` public URL the media files are served from, e.g. `https://api.prawko.pl/media`

`data.sql` is not run in `prod` - seed categories and languages once manually.

## First admin

No endpoint assigns the `ADMIN` role, and `POST /questions` requires it. To create the first admin, register an
account, verify its email and set the role in the database (emails are stored in lowercase):

```sql
UPDATE `user` SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

The role is in the access token, so it applies after signing in again or refreshing.

## Docker

Build the image as in [Running with Docker](configuration.md#running-with-docker), set the profile at runtime, mount
the media directory as a volume and publish no port - put the application on the proxy's network instead
([Reverse proxy](#reverse-proxy)), e.g.

`docker run --network prawko -v /srv/prawko/media:/media -e SPRING_PROFILES_ACTIVE=prod -e MEDIA_DIRECTORY=/media ... prawko-server`.
The container runs as UID 1000, which must be able to read the files in the volume.

## Single instance

Run a single instance. Scheduled jobs (expiring exams, deleting unverified accounts and old refresh tokens) run
separately in every instance, and queued emails are kept in the memory of the instance that queued them.

## Shutdown

On `SIGTERM` the application stops taking requests and finishes the ones in progress for up to 30 seconds
(`spring.lifecycle.timeout-per-shutdown-phase`), then sends the queued emails for up to another 30 seconds
(`spring.task.execution.shutdown.await-termination-period`). Emails still queued after that are lost. Reset and
verification emails can be requested again after the cooldown, an email change by repeating it; a lost identity notice
is not sent again.

The stop timeout of the deployment must be longer than both periods together. `docker stop` waits only 10 seconds by
default, so use e.g. `docker stop -t 70`.

## Reverse proxy

The `prod` profile expects a reverse proxy in front that terminates HTTPS, since the application serves only plain
HTTP. It trusts the `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host` and `X-Forwarded-Port` headers
(`server.forward-headers-strategy: native`), so the logs show the client's IP and `Location` headers carry the public
scheme and host. The proxy must set these headers and append the client's IP to `X-Forwarded-For` (or replace it).
Requests forwarded as HTTPS count as secure, so the application itself sends
`Strict-Transport-Security: max-age=31536000 ; includeSubDomains` (Spring Security's default). The proxy shouldn't add
another one, and every subdomain of the API host must serve HTTPS.

The headers are trusted only from internal addresses (`server.tomcat.remoteip.internal-proxies`): private
`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16` and `fc00::/7`, loopback `127.0.0.0/8` and `::1`, link-local
`169.254.0.0/16` and `fe80::/10`, and the shared `100.64.0.0/10` (carrier-grade NAT, Tailscale). So don't expose the
application directly, also not to a VPN or cloud network in these ranges. A port published with `docker run -p` may
make every client look like it comes from a private address, and then any client could set its IP and the `Location`
host itself (not tested). Keep the application on a network only the proxy can reach, e.g. a Docker network without a
published port.
