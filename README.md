# Prawko Server

## Table of Contents

* [Table of Contents](#table-of-contents)
* [Overview](#overview)
* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [Usage](#usage)

---

## Overview

**Prawko Server** is a REST API Server created with Java 21 and Spring 6 for Prawko projects.

---

## Prerequisites

* Java 21
* Maven

### Debian 13

`sudo apt install openjdk-21-jdk`

`sudo apt install maven`

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

By default, application is using H2 database.
It can be run with MySQL and MariaDB databases.
To configure database connection edit
this [file](https://github.com/turczak/prawko-server/blob/main/src/main/resources/application.properties)
before build.

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
