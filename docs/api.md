# API

Base URL: `http://localhost:8080/`

Application has Swagger, so you can test out the REST API via `/swagger-ui/index.html` (OpenAPI docs at
`/v3/api-docs`). Both are disabled in the `prod` profile. For protected endpoints, sign in with `POST /auth`
([Authentication](authentication.md)) and paste the access token into "Authorize". [`/media`](#endpoints) is served by a resource handler, not a controller, so it's
documented only here.

## Endpoints

Access: **public** - no authentication, **user** - any signed-in user, **self** - the signed-in user acting on their
own data (admins may act on any user), **admin** - admins only.

A request that changes a user (e.g. updating or deleting them, verifying an email, changing or resetting the password,
or a Google or GitHub sign-in that verifies the account) returns `409` when it races another change to the same user;
retry it.

* `/auth`

  | Method | Path                    | Access | Description                                                                                    |
  |--------|-------------------------|--------|------------------------------------------------------------------------------------------------|
  | `POST` | `/auth`                 | public | sign in, returns an access and a refresh token                                                 |
  | `POST` | `/auth/refresh`         | public | exchange a refresh token for a new token pair, the used one is revoked                         |
  | `POST` | `/auth/logout`          | public | revoke a refresh token                                                                         |
  | `POST` | `/auth/google`          | public | sign in with a Google ID token, returns an access and a refresh token                          |
  | `POST` | `/auth/github`          | public | sign in with a GitHub authorization code, returns an access and a refresh token                |
  | `POST` | `/auth/password/forgot` | public | email a password reset link, returns `202` whether the account exists or not                   |
  | `POST` | `/auth/password/reset`  | public | set a new password with the token from the reset link                                          |
  | `POST` | `/auth/verify`          | public | verify an email with a link's token, returns `202` while an email change awaits the other link |
  | `POST` | `/auth/verify/resend`   | public | email a new verification link, returns `202` whatever the account's state                      |

* `/users`

  | Method   | Path                          | Access | Description                                                                                          |
  |----------|-------------------------------|--------|------------------------------------------------------------------------------------------------------|
  | `POST`   | `/users`                      | public | register a new user, emails a verification link                                                      |
  | `GET`    | `/users/me`                   | user   | get the signed-in user (e.g. their ID and role)                                                      |
  | `GET`    | `/users/me/stats`             | user   | get own exam statistics, see [Exam statistics](#exam-statistics)                                     |
  | `DELETE` | `/users/me`                   | user   | delete own account with its exams (requires current password, if none set one with a password reset) |
  | `PATCH`  | `/users/me/password`          | user   | change own password (requires current password, if none set one with a password reset)               |
  | `POST`   | `/users/me/identities/google` | user   | link a Google account (requires current password)                                                    |
  | `DELETE` | `/users/me/identities/google` | user   | unlink the Google account (requires current password)                                                |
  | `POST`   | `/users/me/identities/github` | user   | link a GitHub account (requires current password)                                                    |
  | `DELETE` | `/users/me/identities/github` | user   | unlink the GitHub account (requires current password)                                                |
  | `GET`    | `/users`                      | admin  | get a page of users                                                                                  |
  | `GET`    | `/users/{id}`                 | admin  | get a user                                                                                           |
  | `PATCH`  | `/users/{id}`                 | self   | update a user (email: the user themselves only; own email or username requires current password)     |
  | `DELETE` | `/users/{id}`                 | admin  | delete a user with their exams                                                                       |

  Usernames and emails are unique ignoring case: registering or renaming to one that differs from a taken one only in
  case returns `409`. Emails are stored lowercased, so responses return them lowercased and mails go to the lowercased
  address. On MariaDB (`prod`, default collation) usernames also ignore accents: `żaba` conflicts with `zaba`, and
  signing in with either finds the same user. The H2 database of `dev` and the tests keeps them apart. Emails differing
  only in accents are different addresses on both. Users can still change the capitalization and the accents of their
  own username.

* `/questions`

  | Method | Path                                                | Access | Description                                                                                               |
  |--------|-----------------------------------------------------|--------|-----------------------------------------------------------------------------------------------------------|
  | `POST` | `/questions`                                        | admin  | upload a CSV or XLSX file (`file` multipart part, max 5MB, `413` above it) with questions                 |
  | `GET`  | `/questions`                                        | admin  | get a page of questions                                                                                   |
  | `GET`  | `/questions/random?category={category}&type={type}` | user   | random question for practice, with answers' `correct` flag; `category` and `type` are random when missing |
  | `GET`  | `/questions/{id}`                                   | user   | get a question; answers' `correct` flag is included only for admins                                       |

  An upload over about 20MB (`server.tomcat.max-swallow-size`) loses the connection instead of getting the `413`, so a
  rejected request can't hold a thread until an arbitrarily large body is read.

  The part's content type picks the format: `text/csv` (UTF-8, comma-separated) or
  `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` (`.xlsx`); any other type returns `415`. Of an
  XLSX workbook only the `katalog` sheet is imported (the catalogue's `W trakcie weryfikacji` sheet is ignored): it's
  converted to CSV, numbers in plain notation, and imported like an uploaded CSV, with the same rules and error
  messages, and rows numbered as in the sheet. A file that isn't a valid workbook returns `400` `Invalid XLSX file.`,
  one without a `katalog` sheet `Invalid XLSX file: no sheet 'katalog'.`.

  The CSV uses the column layout of the government's question catalogue from 07.2026 (`Numer pytania`, `Pytanie`,
  `Odpowiedź A`-`C`, `Pytanie [EN]`, `Odpowiedź A [EN]`-`C [EN]`, `Pytanie [D]`, `Odpowiedź A [D]`-`C [D]`,
  `Poprawna odp`, `Media`, `Zakres struktury`, `Liczba punktów`, `Kategorie`), in any order. A file missing one of
  these columns is rejected; other columns are ignored. Rows with an empty `Numer pytania` (such as blank rows at the
  end of the sheet) are skipped. The catalogue currently in use is kept in
  `data/KATALOG_dla_kandydatów_na_kierowców_072026.xlsx` and can be uploaded as is.

  The import is all or nothing: a malformed file returns `400` and saves no questions. Errors name the row as a
  spreadsheet numbers it: the header is row 1, and a value spanning several lines is still one row. Every name in the
  `Kategorie` column must be a category (`A`, `A1`, …, `PT`, case-sensitive); an unknown or empty one fails the file,
  and the message lists each offending row and value together with the allowed categories, e.g. `Invalid CSV file:
  row 12: unknown category 'Z'; row 40: unknown category ''. Allowed: A, A1, …`.

  The question type follows `Poprawna odp`: `T` or `N` makes a `BASIC` (yes/no) question, `A`, `B` or `C` a `SPECIAL`
  one; any other value fails the file the same way (`row 7: unknown correct answer 'X', expected one of T, N, A, B, C`).
  `Zakres struktury` (`PODSTAWOWY`/`SPECJALISTYCZNY`, any case) doesn't decide it: when it names the other type, is
  misspelled or empty, the question is imported with the type of its answer and a warning is logged, since the
  catalogue has such rows.

  A `BASIC` (yes/no) question has two answers without translations, always in this order: yes, then no. The one named
  by `Poprawna odp` (`T` for yes, `N` for no) is correct. A `SPECIAL` question has answers `A`, `B` and `C` in that
  order.

  Hiding the `correct` flag (here and on active exams) only keeps it out of those views, it isn't a security boundary:
  the question bank is public and `GET /questions/random` includes the flag for practice.

  `GET /questions/random` picks a missing `category` or `type` at random, each value equally likely. The full question
  bank has questions of both types in every category; after a partial import the picked combination can be empty and
  the request returns `404` naming it.

* `/exams`

  | Method | Path                                     | Access | Description                                                                                 |
  |--------|------------------------------------------|--------|---------------------------------------------------------------------------------------------|
  | `POST` | `/exams`                                 | self   | create a new exam for the `userId` from the request body                                    |
  | `GET`  | `/exams?userId={id}&category={category}` | self   | get a page of the user's exams history, newest first; `category` is optional and filters it |
  | `GET`  | `/exams/{id}`                            | self   | get an exam; answers' `correct` flag is included only once the exam is not `active`         |
  | `POST` | `/exams/{id}/answers`                    | owner  | answer a question of an active exam, see [Solving an exam](#solving-an-exam)                |
  | `POST` | `/exams/{id}/finish`                     | owner  | finish an active exam and calculate its score, see [Solving an exam](#solving-an-exam)      |

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
  and `mediaType` is `IMAGE` or `VIDEO` (from the file extension); both are `null` for a question without media, and
  `mediaType` alone is `null` when the extension is neither an image nor a video. Moving the files to a CDN only
  changes `media.base-url`. To add media, put the file into the media directory under the name from the `Media` column
  of the imported question file - except `.wmv` videos: the import renames them to `.webm`, so convert them to WebM and
  store them as `<name>.webm`. Names may contain spaces, commas, parentheses and Polish letters (`JAZDA NOCĄorg.webm`);
  `mediaUrl` percent-encodes them. The name must match byte for byte: a few catalogue names spell accents as separate
  combining characters (Unicode NFD), and a file named with the precomposed letters isn't found.

`POST` endpoints creating a resource return `201 Created` with its URL in the `Location` header.

## Solving an exam

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
| `400`  | `answerId` is missing or the body is missing or malformed (answers only), or the exam ID is not positive                     |
| `403`  | the exam belongs to another user                                                                                             |
| `404`  | the exam doesn't exist, or the answer doesn't belong to any of its questions                                                 |
| `409`  | the exam is no longer `active` or its time has expired (answers only), or it was modified by a concurrent request - retry it |

## Exam statistics

`GET /users/me/stats` summarizes the signed-in user's finished exams, overall and per category; active exams are not
counted. An exam is passed with a `score` of at least 68 (out of 74). Categories without finished exams are left out.

```json
{
  "examsTaken": 12,
  "examsPassed": 7,
  "averageScore": 63.5,
  "bestScore": 72,
  "categories": [
    {"category": "A", "examsTaken": 2, "examsPassed": 1, "averageScore": 60.5, "bestScore": 68},
    {"category": "B", "examsTaken": 10, "examsPassed": 6, "averageScore": 64.1, "bestScore": 72}
  ]
}
```

Without finished exams the counts are `0`, `averageScore` and `bestScore` are `null` and `categories` is empty.

## Pagination

List endpoints (`GET /users`, `GET /questions`, `GET /exams`) are paginated with query parameters:

* `page` - page number, starting from `0`
* `size` - page size, default `20`, max `100`
* `sort` - property and optional direction, e.g. `sort=id,desc`; can be repeated
