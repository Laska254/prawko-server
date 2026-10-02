# Prawko Server

## Table of Contents

* [Table of Contents](#table-of-contents)
* [Overview](#overview)
* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [Usage](#usage)
* [Emails (Mailpit)](#emails-mailpit)

---

## Overview

**Prawko Server** is a REST API Server created with Java 21 and Spring 7 for Prawko projects.

---

## Prerequisites

* Java 21
* Maven
* Docker (optional, to catch emails locally with [Mailpit](#emails-mailpit))

### Debian 13

`sudo apt install openjdk-21-jdk`

`sudo apt install maven`

`sudo apt install docker.io`

To run `docker` without `sudo`, add yourself to the `docker` group and log out and back in (the group grants
root-equivalent access):

`sudo usermod -aG docker $USER`

### Windows

`winget install Microsoft.OpenJDK.21`

You can install Maven via terminal with **Scoop**. If you don't have Scoop installed you can do it running this in
terminal:

`Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
Invoke-RestMethod -Uri https://get.scoop.sh | Invoke-Expression`

Maven installation

`scoop install main/maven`

## Installation

Clone repository

`git@github.com:turczak/prawko-server.git`

`cd prawko-server`

The application has two profiles:

* `dev` (default) - in-memory H2 database recreated and seeded with `data.sql` on every start, CORS allowed from
  `localhost`, emails sent to SMTP on `localhost:1025` (see [Emails (Mailpit)](#emails-mailpit)).
* `prod` - MariaDB, schema updated by Hibernate, Swagger disabled. Requires environment variables:
    + `DB_URL` e.g. `jdbc:mariadb://localhost:3306/prawko`
    + `DB_USERNAME`
    + `DB_PASSWORD`
    + `CORS_ALLOWED_ORIGIN_PATTERNS` comma-separated, e.g. `https://prawko.pl`
    + `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` SMTP server (with auth and STARTTLS)
    + `MAIL_FROM` sender address, e.g. `no-reply@prawko.pl`
    + `PASSWORD_RESET_URL` frontend page the reset link points to, e.g. `https://prawko.pl/reset-password`

  `data.sql` is not run in `prod` - seed categories and languages once manually.

Select the profile with `SPRING_PROFILES_ACTIVE=prod` or `--spring.profiles.active=prod`.

Test

`./mvnw clean install`

Build with Maven

`./mvnw package`

Run

`./mvnw spring-boot:run`

## Usage

Base URL: `http://localhost:8080/`

### Endpoints

Application has Swagger so you can test out REST API via `/swagger-ui/index.html`

* `/questions`
    + `POST` upload csv file with questions
    + `GET` get all questions
    + `GET /{id}` get question
* `/users`
    + `POST` register new user
    + `GET /{id}` get a user
    + `GET` get all users
    + `PATCH /{id}` update a user
    + `DELETE /{id}` delete a user
* `/exams`
    + `POST` create new exam
    + `GET /{id}` get an exam

## Emails (Mailpit)

In the `dev` profile emails (e.g. password reset links) are sent to SMTP on `localhost:1025`. Run
[Mailpit](https://mailpit.axllent.org/) to catch them - nothing is delivered to real inboxes.

Run Mailpit (SMTP on port 1025, web UI on port 8025)

`docker run --rm -p 1025:1025 -p 8025:8025 axllent/mailpit`

Then start the application and open the inbox at `http://localhost:8025`.

Without Mailpit running the application still works, but emails are not sent - the failure is only logged.

Mailpit also has a REST API, e.g. `GET http://localhost:8025/api/v1/message/latest` returns the latest email
(its body is in the `Text` field), so an API client can read the reset token from it.
