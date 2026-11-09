# Configuration

## Profiles

Configuration files in `src/main/resources` (except `application-test.yaml`):

* `application.yaml` - defaults shared by all profiles, no passwords or URLs.
* `src/test/resources/application-test.yaml` - `test` profile, used by local and CI tests (activated with
  `@ActiveProfiles("test")`). On the test classpath only, so it is not packaged into the JAR. In-memory H2 database
  recreated and seeded with `data.sql` (categories and languages).
* `application-dev.yaml` - `dev` profile (default), not in the repository - your local connection details for
  development and manual verification, see [Local configuration](#local-configuration). Left out of the JAR, so
  it never ends up in an image.
* `application-prod.yaml` - `prod` profile: MariaDB, schema updated by Hibernate, Swagger disabled, runs behind a
  [proxy](deployment.md#reverse-proxy). Holds no credentials - all connection details come from
  [environment variables](deployment.md#environment-variables).

Select the profile with `SPRING_PROFILES_ACTIVE=<profile>` or `--spring.profiles.active=<profile>`.

## Local configuration

Create `src/main/resources/application-dev.yaml` (ignored by Git) before running the application locally. The
example below uses an in-memory H2 database recreated and seeded with `data.sql` on every start, allows CORS from
`localhost` and sends emails to SMTP on `localhost:1025` (see [Emails (Mailpit)](#emails-mailpit)):

```yaml
cors:
  allowed-origin-patterns: "http://localhost:[*],http://127.0.0.1:[*]"

spring:
  datasource:
    url: jdbc:h2:mem:prawko
    username: sa
    password: ""
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
    defer-datasource-initialization: true

  mail:
    host: localhost
    port: 1025

mail:
  from: no-reply@prawko.local

password-reset:
  url: http://localhost:5173/auth/password/reset

email-verification:
  url: http://localhost:5173/auth/verify

media:
  base-url: http://localhost:8080/media
```

To use a local MariaDB instead, replace the datasource, e.g. `url: jdbc:mariadb://localhost:3306/prawko` with your
username and password, and set `spring.jpa.hibernate.ddl-auto` to `update` (`data.sql` is then not run - seed
categories and languages once manually).

To test [signing in with Google](authentication.md#google-sign-in) locally, create an OAuth client ID of type *Web application* in the
[Google Cloud console](https://console.cloud.google.com/apis/credentials). Under *Authorized JavaScript origins* add the
client's origin, for prawko-web both `http://localhost` and `http://localhost:5173`. Add the ID to
`application-dev.yaml` (`GOOGLE_CLIENT_IDS` is only read in `prod`) and restart the application:

```yaml
google:
  client-ids: <client-id>.apps.googleusercontent.com
```

A made-up token then answers `401`; `404` means the ID isn't configured:

`curl -i -X POST http://localhost:8080/auth/google -H 'Content-Type: application/json' -d '{"idToken":"x"}'`

A real token needs a client: set the same ID as `VITE_GOOGLE_CLIENT_ID` in prawko-web's `.env.development.local` and use
the Google button on its `/auth` page. While the app's consent screen is in *Testing* status, only its test users can
sign in.

## Running with Docker

Run with Docker (after `./mvnw package`)

`docker build -t prawko-server .`

`docker run -p 8080:8080 -v "$PWD/src/main/resources/application-dev.yaml:/config/application-dev.yaml:ro" -e SPRING_CONFIG_ADDITIONAL_LOCATION=file:/config/ prawko-server`

With no profile the image falls back to `dev`, whose config isn't in the image, so without the mounted file it exits
with status 1. Inside the container `localhost` is the container itself, so it can't reach Mailpit on `localhost:1025`.
Put both containers on one network instead: `docker network create prawko`, start Mailpit with
`--network prawko --name mailpit` and the application with `--network prawko -e SPRING_MAIL_HOST=mailpit`. For `prod`,
see [Deployment](deployment.md#docker).

## Emails (Mailpit)

In the `dev` profile emails (e.g. email verification and password reset links) are sent to SMTP on `localhost:1025`. Run
[Mailpit](https://mailpit.axllent.org/) to catch them - nothing is delivered to real inboxes.

Run Mailpit (SMTP on port 1025, web UI on port 8025)

`docker run --rm -p 1025:1025 -p 8025:8025 axllent/mailpit`

Then start the application and open the inbox at `http://localhost:8025`.

Without Mailpit running the application still works, but emails are not sent - the failure is only logged.

Mailpit also has a REST API, e.g. `GET http://localhost:8025/api/v1/message/latest` returns the latest email
(its body is in the `Text` field), so an API client can read the verification or reset token from it.

Password reset links are valid for 15 minutes; another reset email for the same account can be requested after
1 minute.

Email verification links are valid for 24 hours; another one can be requested after 1 minute. Unverified accounts
with expired links are deleted every hour.

Reset, verification and identity notice emails are processed in the background on a bounded pool
(`spring.task.execution.pool.*`: 8 threads, 100 queued). When it's full, `/auth/password/forgot`, `/auth/verify/resend`,
`/users` (registration), email changes with `PATCH /users/{id}`, `POST`/`DELETE /users/me/identities/*` and a first
`/auth/google` sign-in that links an account to a user with other linked accounts answer `503`.
