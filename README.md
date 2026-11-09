# Prawko Server

[![codecov](https://codecov.io/gh/Laska254/prawko-server/graph/badge.svg)](https://codecov.io/gh/Laska254/prawko-server)
[![Release](https://img.shields.io/github/v/tag/Laska254/prawko-server?sort=semver&filter=v%2A&label=Release)](https://github.com/Laska254/prawko-server/releases/latest)
[![Javadoc](https://img.shields.io/github/v/tag/Laska254/prawko-server?sort=semver&filter=v%2A&label=Javadoc&color=blue)](https://laska254.github.io/prawko-server/)
![Java](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2FLaska254%2Fprawko-server%2Fmain%2Fpom.xml&query=%2F%2F*%5Blocal-name%28%29%3D'java.version'%5D&label=Java&color=orange)
![Spring Boot](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2FLaska254%2Fprawko-server%2Fmain%2Fpom.xml&query=%2F*%5Blocal-name%28%29%3D'project'%5D%2F*%5Blocal-name%28%29%3D'parent'%5D%2F*%5Blocal-name%28%29%3D'version'%5D&label=Spring%20Boot&color=brightgreen)

## Table of Contents

* [Overview](#overview)
* [Documentation](#documentation)
* [Prerequisites](#prerequisites)
* [Quick start](#quick-start)

---

## Overview

**Prawko Server** is a REST API Server created with Java 21 and Spring Boot 4 (Spring 7) for Prawko projects.

---

## Documentation

* [Configuration](docs/configuration.md) - profiles, local `application-dev.yaml`, running with Docker, catching
  emails with Mailpit
* [Authentication](docs/authentication.md) - access and refresh tokens, Google and GitHub sign-in, linking accounts,
  email verification and changes
* [API](docs/api.md) - Swagger, endpoints, solving an exam, exam statistics, pagination
* [Deployment](docs/deployment.md) - `prod` environment variables, first admin, Docker, shutdown, reverse proxy

---

## Prerequisites

* Java 21
* Docker (optional, to catch emails locally with [Mailpit](docs/configuration.md#emails-mailpit))

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

## Quick start

Clone repository

`git clone https://github.com/Laska254/prawko-server.git`

`cd prawko-server`

Test (unit and integration tests, coverage check, javadoc)

`./mvnw clean verify`

Build with Maven

`./mvnw package`

Run (requires `application-dev.yaml`, see [Local configuration](docs/configuration.md#local-configuration))

`./mvnw spring-boot:run`

Then open Swagger at `http://localhost:8080/swagger-ui/index.html`, see [API](docs/api.md).
