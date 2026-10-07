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
* [Usage](#usage)
    + [Authentication](#authentication)
    + [Endpoints](#endpoints)
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

The application has two profiles:

* `dev` (default) - in-memory H2 database recreated and seeded with `data.sql` (categories and languages) on every
  start, CORS allowed from `localhost`, emails sent to SMTP on `localhost:1025` (see [Emails (Mailpit)](#emails-mailpit)).
* `prod` - MariaDB, schema updated by Hibernate, Swagger disabled. Requires environment variables:
    + `DB_URL` e.g. `jdbc:mariadb://localhost:3306/prawko`
    + `DB_USERNAME`
    + `DB_PASSWORD`
    + `CORS_ALLOWED_ORIGIN_PATTERNS` comma-separated, e.g. `https://prawko.pl`
    + `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` SMTP server (with auth and STARTTLS)
    + `MAIL_FROM` sender address, e.g. `no-reply@prawko.pl`
    + `PASSWORD_RESET_URL` frontend page the reset link points to, e.g. `https://prawko.pl/auth/password/reset`

  `data.sql` is not run in `prod` - seed categories and languages once manually.

Select the profile with `SPRING_PROFILES_ACTIVE=prod` or `--spring.profiles.active=prod`.

Test (unit and integration tests, coverage check, javadoc)

`./mvnw clean verify`

Build with Maven

`./mvnw package`

Run

`./mvnw spring-boot:run`

Run with Docker (after `./mvnw package`)

`docker build -t prawko-server .`

`docker run -p 8080:8080 prawko-server`

---

## Usage

Base URL: `http://localhost:8080/`

Application has Swagger, so you can test out the REST API via `/swagger-ui/index.html` (OpenAPI docs at
`/v3/api-docs`). Both are disabled in the `prod` profile.

### Authentication

The API is stateless and uses HTTP Basic - every request that requires authentication must carry the
`Authorization: Basic ...` header. You can sign in with either the username or the email address.

There are two roles: `USER` (every registered user) and `ADMIN`. `ADMIN` includes all `USER` permissions.

### Endpoints

Access: **public** - no authentication, **user** - any signed-in user, **self** - the signed-in user acting on their
own data (admins may act on any user), **admin** - admins only.

* `/auth`

  | Method | Path                    | Access | Description                                                                  |
  |--------|-------------------------|--------|------------------------------------------------------------------------------|
  | `POST` | `/auth`                 | public | verify credentials (sign-in)                                                 |
  | `POST` | `/auth/password/forgot` | public | email a password reset link, returns `202` whether the account exists or not |
  | `POST` | `/auth/password/reset`  | public | set a new password with the token from the reset link                        |

* `/users`

  | Method   | Path                 | Access | Description                                     |
  |----------|----------------------|--------|-------------------------------------------------|
  | `POST`   | `/users`             | public | register a new user                             |
  | `GET`    | `/users/me`          | user   | get the signed-in user (e.g. their ID and role) |
  | `PATCH`  | `/users/me/password` | user   | change own password (requires current password) |
  | `GET`    | `/users`             | admin  | get a page of users                             |
  | `GET`    | `/users/{id}`        | admin  | get a user                                      |
  | `PATCH`  | `/users/{id}`        | self   | update a user                                   |
  | `DELETE` | `/users/{id}`        | admin  | delete a user                                   |

* `/questions`

  | Method | Path              | Access | Description                                                                |
  |--------|-------------------|--------|----------------------------------------------------------------------------|
  | `POST` | `/questions`      | admin  | upload a CSV file (`file` multipart part, max 5MB) with questions          |
  | `GET`  | `/questions`      | admin  | get a page of questions                                                    |
  | `GET`  | `/questions/{id}` | user   | get a question; answers' `correct` flag is included only for admins        |

* `/exams`

  | Method | Path                 | Access | Description                                                                         |
  |--------|----------------------|--------|-------------------------------------------------------------------------------------|
  | `POST` | `/exams`             | self   | create a new exam for the `userId` from the request body                            |
  | `GET`  | `/exams?userId={id}` | self   | get a page of the user's exams history, newest first                                |
  | `GET`  | `/exams/{id}`        | self   | get an exam; answers' `correct` flag is included only once the exam is not `active` |

`POST` endpoints creating a resource return `201 Created` with its URL in the `Location` header.

### Pagination

List endpoints (`GET /users`, `GET /questions`, `GET /exams`) are paginated with query parameters:

* `page` - page number, starting from `0`
* `size` - page size, default `20`, max `100`
* `sort` - property and optional direction, e.g. `sort=id,desc`; can be repeated

---

## Emails (Mailpit)

In the `dev` profile emails (e.g. password reset links) are sent to SMTP on `localhost:1025`. Run
[Mailpit](https://mailpit.axllent.org/) to catch them - nothing is delivered to real inboxes.

Run Mailpit (SMTP on port 1025, web UI on port 8025)

`docker run --rm -p 1025:1025 -p 8025:8025 axllent/mailpit`

Then start the application and open the inbox at `http://localhost:8025`.

Without Mailpit running the application still works, but emails are not sent - the failure is only logged.

Mailpit also has a REST API, e.g. `GET http://localhost:8025/api/v1/message/latest` returns the latest email
(its body is in the `Text` field), so an API client can read the reset token from it.

Password reset links are valid for 15 minutes; another reset email for the same account can be requested after
1 minute.
