# Prawko Server

[![codecov](https://codecov.io/gh/Laska254/prawko-server/graph/badge.svg)](https://codecov.io/gh/Laska254/prawko-server)
[![Release](https://img.shields.io/github/v/tag/Laska254/prawko-server?sort=semver&filter=v%2A&label=Release)](https://github.com/Laska254/prawko-server/releases/latest)
[![Javadoc](https://img.shields.io/github/v/tag/Laska254/prawko-server?sort=semver&filter=v%2A&label=Javadoc&color=blue)](https://laska254.github.io/prawko-server/)
![Java](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2FLaska254%2Fprawko-server%2Fmain%2Fpom.xml&query=%2F%2F*%5Blocal-name%28%29%3D'java.version'%5D&label=Java&color=orange)
![Spring Boot](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2FLaska254%2Fprawko-server%2Fmain%2Fpom.xml&query=%2F*%5Blocal-name%28%29%3D'project'%5D%2F*%5Blocal-name%28%29%3D'parent'%5D%2F*%5Blocal-name%28%29%3D'version'%5D&label=Spring%20Boot&color=brightgreen)

## Table of Contents

* [Table of Contents](#table-of-contents)
* [Overview](#overview)
* [Prerequisites](#prerequisites)
* [Installation](#installation)
    + [Local configuration](#local-configuration)
* [Usage](#usage)
    + [Authentication](#authentication)
    + [Endpoints](#endpoints)
    + [Solving an exam](#solving-an-exam)
    + [Pagination](#pagination)
* [Emails (Mailpit)](#emails-mailpit)

---

## Overview

**Prawko Server** is a REST API Server created with Java 21 and Spring Boot 4 (Spring 7) for Prawko projects.

---

## Prerequisites

* Java 21
* Docker (optional, to catch emails locally with [Mailpit](#emails-mailpit))

Maven doesn't have to be installed - the repository includes the Maven Wrapper (`./mvnw`, `mvnw.cmd` on Windows).

### Debian 13

`sudo apt install openjdk-21-jdk`

`sudo apt install docker.io`

To run `docker` without `sudo`, add yourself to the `docker` group and log out and back in (the group grants
root-equivalent access):

`sudo usermod -aG docker $USER`

### Windows

`winget install Microsoft.OpenJDK.21`

---

## Installation

Clone repository

`git clone https://github.com/Laska254/prawko-server.git`

`cd prawko-server`

Configuration files in `src/main/resources` (except `application-test.yaml`):

* `application.yaml` - defaults shared by all profiles, no passwords or URLs.
* `src/test/resources/application-test.yaml` - `test` profile, used by local and CI tests (activated with
  `@ActiveProfiles("test")`). On the test classpath only, so it is not packaged into the JAR. In-memory H2 database
  recreated and seeded with `data.sql` (categories and languages).
* `application-dev.yaml` - `dev` profile (default), not in the repository - your local connection details for
  development and manual verification, see [Local configuration](#local-configuration).
* `application-prod.yaml` - `prod` profile: MariaDB, schema updated by Hibernate, Swagger disabled. Holds no
  credentials - all connection details come from environment variables:
    + `DB_URL` e.g. `jdbc:mariadb://localhost:3306/prawko`
    + `DB_USERNAME`
    + `DB_PASSWORD`
    + `CORS_ALLOWED_ORIGIN_PATTERNS` comma-separated, e.g. `https://prawko.pl`
    + `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` SMTP server (with auth and STARTTLS)
    + `MAIL_FROM` sender address, e.g. `no-reply@prawko.pl`
    + `PASSWORD_RESET_URL` frontend page the reset link points to, e.g. `https://prawko.pl/auth/password/reset`
    + `EMAIL_VERIFICATION_URL` frontend page the email verification link points to, e.g. `https://prawko.pl/auth/verify`
    + `MEDIA_DIRECTORY` directory with question media files, e.g. `/media` (a Docker volume)
    + `MEDIA_BASE_URL` public URL the media files are served from, e.g. `https://api.prawko.pl/media`

  `data.sql` is not run in `prod` - seed categories and languages once manually.

Select the profile with `SPRING_PROFILES_ACTIVE=<profile>` or `--spring.profiles.active=<profile>`.

Test (unit and integration tests, coverage check, javadoc)

`./mvnw clean verify`

Build with Maven

`./mvnw package`

Run (requires `application-dev.yaml`, see [Local configuration](#local-configuration))

`./mvnw spring-boot:run`

Run with Docker (after `./mvnw package`)

`docker build -t prawko-server .`

`docker run -p 8080:8080 prawko-server`

In the `prod` profile mount the media directory as a volume, e.g.
`docker run -p 8080:8080 -v /srv/prawko/media:/media -e MEDIA_DIRECTORY=/media ... prawko-server`.

### Local configuration

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

---

## Usage

Base URL: `http://localhost:8080/`

Application has Swagger, so you can test out the REST API via `/swagger-ui/index.html` (OpenAPI docs at
`/v3/api-docs`). Both are disabled in the `prod` profile. [`/media`](#endpoints) is served by a resource handler, not
a controller, so it's documented only here.

### Authentication

The API is stateless and uses HTTP Basic - every request that requires authentication must carry the
`Authorization: Basic ...` header. You can sign in with either the username or the email address.

There are two roles: `USER` (every registered user) and `ADMIN`. `ADMIN` includes all `USER` permissions.

A new account must verify its email address before signing in. Registration emails a link to the frontend page
(`email-verification.url`) with a `token` query parameter, which the page sends to `POST /auth/verify` as
`{"token": "..."}`. Until then `POST /auth` answers `403` (only with the correct password, otherwise `401`) and every
other authenticated request `401`. Another link can be requested with `POST /auth/verify/resend`. Accounts not
verified before their link expires are deleted, so the username and email can be registered again.

Changing the email with `PATCH /users/{id}` works the same way: the new address is only stored as pending and a link
is emailed to it (`pendingEmail` in the user response), and a confirmation link to the current address, so a stolen
password alone can't move the account to another inbox. Only the user themselves may change their email - admins get
`403`, so a stolen admin account can't take over other accounts this way either. Both links go to
`POST /auth/verify`, in any order; the user response tells which are still unopened (`awaitingNewEmailConfirmation`,
`awaitingCurrentEmailConfirmation`). The current email stays in use (sign-in, password reset) until both links are
used. Changing or resetting the password cancels a pending change, and so does sending the current email in the
`PATCH`. For new links send the `PATCH` again after the cooldown - within it the `PATCH` answers `429` and changes
nothing.

Deleting an account (`DELETE /users/me`, or `DELETE /users/{id}` by an admin) removes its exams and invalidates any
pending reset or verification links; the username and email can be registered again.

### Endpoints

Access: **public** - no authentication, **user** - any signed-in user, **self** - the signed-in user acting on their
own data (admins may act on any user), **admin** - admins only.

* `/auth`

  | Method | Path                    | Access | Description                                                                                    |
  |--------|-------------------------|--------|------------------------------------------------------------------------------------------------|
  | `POST` | `/auth`                 | public | verify credentials (sign-in)                                                                   |
  | `POST` | `/auth/password/forgot` | public | email a password reset link, returns `202` whether the account exists or not                   |
  | `POST` | `/auth/password/reset`  | public | set a new password with the token from the reset link                                          |
  | `POST` | `/auth/verify`          | public | verify an email with a link's token, returns `202` while an email change awaits the other link |
  | `POST` | `/auth/verify/resend`   | public | email a new verification link, returns `202` whatever the account's state                      |

* `/users`

  | Method   | Path                 | Access | Description                                                   |
  |----------|----------------------|--------|---------------------------------------------------------------|
  | `POST`   | `/users`             | public | register a new user, emails a verification link               |
  | `GET`    | `/users/me`          | user   | get the signed-in user (e.g. their ID and role)               |
  | `DELETE` | `/users/me`          | user   | delete own account with its exams (requires current password) |
  | `PATCH`  | `/users/me/password` | user   | change own password (requires current password)               |
  | `GET`    | `/users`             | admin  | get a page of users                                           |
  | `GET`    | `/users/{id}`        | admin  | get a user                                                    |
  | `PATCH`  | `/users/{id}`        | self   | update a user (email: the user themselves only)               |
  | `DELETE` | `/users/{id}`        | admin  | delete a user with their exams                                |

* `/questions`

  | Method | Path                                                | Access | Description                                                                                               |
  |--------|-----------------------------------------------------|--------|-----------------------------------------------------------------------------------------------------------|
  | `POST` | `/questions`                                        | admin  | upload a CSV file (`file` multipart part, max 5MB) with questions                                         |
  | `GET`  | `/questions`                                        | admin  | get a page of questions                                                                                   |
  | `GET`  | `/questions/random?category={category}&type={type}` | user   | random question for practice, with answers' `correct` flag; `category` and `type` are random when missing |
  | `GET`  | `/questions/{id}`                                   | user   | get a question; answers' `correct` flag is included only for admins                                       |

  Hiding the `correct` flag (here and on active exams) only keeps it out of those views, it isn't a security boundary:
  the question bank is public and `GET /questions/random` includes the flag for practice.

  `GET /questions/random` picks a missing `category` or `type` at random, each value equally likely. The full question
  bank has questions of both types in every category; after a partial import the picked combination can be empty and
  the request returns `404` naming it.

* `/exams`

  | Method | Path                  | Access | Description                                                                            |
  |--------|-----------------------|--------|----------------------------------------------------------------------------------------|
  | `POST` | `/exams`              | self   | create a new exam for the `userId` from the request body                               |
  | `GET`  | `/exams?userId={id}`  | self   | get a page of the user's exams history, newest first                                   |
  | `GET`  | `/exams/{id}`         | self   | get an exam; answers' `correct` flag is included only once the exam is not `active`    |
  | `POST` | `/exams/{id}/answers` | owner  | answer a question of an active exam, see [Solving an exam](#solving-an-exam)           |
  | `POST` | `/exams/{id}/finish`  | owner  | finish an active exam and calculate its score, see [Solving an exam](#solving-an-exam) |

* `/media`

  | Method | Path                | Access | Description                                      |
  |--------|---------------------|--------|--------------------------------------------------|
  | `GET`  | `/media/{filename}` | public | question image or video file                     |
  | `HEAD` | `/media/{filename}` | public | same headers as `GET` (size, `ETag`, …), no body |

  Files are served from `media.directory` (default `media` in the working directory, ignored by Git; `MEDIA_DIRECTORY`
  in `prod`) with the content type from the extension. They're public like the question bank, so image loaders and
  video players need no credentials. Responses are cacheable for `media.cache-max-age` (`1d`), carry `ETag` and
  `Last-Modified` (revalidation answers `304`) and support range requests (`206`), so videos can be streamed and
  seeked. A missing file returns `404`.

  Questions don't expose filenames: `mediaUrl` is the file's absolute URL (`media.base-url` followed by the filename)
  and `mediaType` is `IMAGE` or `VIDEO` (from the file extension); both are `null` for a question without media.
  Moving the files to a CDN only changes `media.base-url`. To add media, put the file into the media directory under
  the name from the `Media` column of the imported question CSV.

`POST` endpoints creating a resource return `201 Created` with its URL in the `Location` header.

### Solving an exam

Submit the ID of the chosen answer, e.g. `POST /exams/7/answers` with `{"answerId": 42}`; it returns `204 No Content`.
The question is determined by the answer, so answering the same question again replaces the previous answer. The
submitted answers are returned in `userAnswers` of `GET /exams/{id}`.

When done, finish the exam with `POST /exams/7/finish` (no body); it returns `200 OK` with the finished exam. The exam
is no longer `active`, so no more answers can be submitted, its `score` is the sum of `points` of the correctly
answered questions (unanswered questions give no points) and answers' `correct` flag is included.

An exam lasts 25 minutes: `expires` (`created` + 25 minutes) is the deadline for answers. Answers submitted after it
return `409`, and the exam is finished automatically within `exam.expiry-check-interval` (`30s`).

Timestamps (`created`, `updated`, `expires`) are ISO-8601 instants in UTC, e.g. `2026-10-23T12:00:00Z`, independent
of the server's time zone. Clients can estimate their clock offset from the `Date` response header (exposed to
cross-origin scripts via CORS) to show the remaining time correctly.

| Status | When                                                                                                                         |
|--------|------------------------------------------------------------------------------------------------------------------------------|
| `400`  | `answerId` or the body is missing (answers only), or the exam ID is not positive                                             |
| `403`  | the exam belongs to another user                                                                                             |
| `404`  | the exam doesn't exist, or the answer doesn't belong to any of its questions                                                 |
| `409`  | the exam is no longer `active` or its time has expired (answers only), or it was modified by a concurrent request - retry it |

### Pagination

List endpoints (`GET /users`, `GET /questions`, `GET /exams`) are paginated with query parameters:

* `page` - page number, starting from `0`
* `size` - page size, default `20`, max `100`
* `sort` - property and optional direction, e.g. `sort=id,desc`; can be repeated

---

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

Reset and verification emails are processed in the background on a bounded pool (`spring.task.execution.pool.*`: 8 threads, 100
queued). When it's full, `/auth/password/forgot`, `/auth/verify/resend`, `/users` (registration) and email changes
with `PATCH /users/{id}` answer `503`.
