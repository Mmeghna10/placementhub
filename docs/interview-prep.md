# PlacementHub: interview preparation

Every answer below points at real code or a real test in this repository, so it can be backed up live ("let me show you"). File paths are relative to `backend/src/main/java/com/placementhub/backend/` unless they start with another folder.

**Contents:**
- [Two-minute project explanation](#two-minute-project-explanation)
- [Five-minute technical walkthrough](#five-minute-technical-walkthrough)
- [Interview questions](#interview-questions) 1–14
- [Numbers worth remembering](#numbers-worth-remembering)
- [Honest limitations](#honest-limitations-say-these-before-you-are-asked)

---

## Two-minute project explanation

> PlacementHub is a campus placement portal for three kinds of users. Students find jobs they're eligible for, apply, and book interview slots. Recruiters post jobs and move applicants through a hiring pipeline. The placement cell, the admins, approve postings.
>
> It's a Spring Boot and React application on MySQL, deployed on Azure: App Service for the API, Static Web Apps for the frontend, and managed MySQL.
>
> I first built it as a working CRUD app. Then I audited it like a production system and fixed what I found, one milestone at a time. Three things I'm proud of:
>
> **First, correctness under concurrency.** I wrote tests that fire 20 simultaneous requests at real MySQL. They proved a double-clicked Apply button could create 12 duplicate applications. Now the database enforces each rule:
> - a unique key for duplicate applications;
> - an atomic conditional update so interview slots can't be overbooked;
> - optimistic locking so two recruiters can't silently overwrite each other.
>
> I also load-tested it with k6. 200 students clicking Apply three times at the same instant left exactly 200 applications.
>
> **Second, an engineering baseline:**
> - Flyway migrations instead of Hibernate changing the schema;
> - stateless JWT security with ownership checks;
> - rate limiting on login, registration and the AI chatbot;
> - request ids in every log line;
> - fixed N+1 queries, with tests that count SQL statements.
>
> **Third, safe delivery.** Docker Compose runs the whole stack with one command. Every push runs about 160 backend and frontend tests, a container vulnerability scan, and only then deploys. That scan found real critical CVEs in my Spring Boot version, and I upgraded.
>
> I deliberately kept it a modular monolith. One database gives me transactions and locks for free, and the measured load doesn't need anything more.

---

## Five-minute technical walkthrough

Use the README's diagrams as the visual. A suggested order, about 40 seconds per step:

1. **Shape of the system (architecture diagram).**
   - React SPA → Spring Boot → MySQL; Gemini is called server-side.
   - Inside the backend: filters → controllers → services → repositories.
   - The **service layer owns the transactions and business rules**. Controllers stay thin.
2. **A request, end to end: a student applies** (`POST /api/students/jobs/{id}/apply`).
   - `RequestIdFilter` gives the request an id.
   - `JwtFilter` verifies the token and reloads the user from the database.
   - Security requires `ROLE_STUDENT`.
   - `StudentService.applyToJob` runs in one transaction:
     - reads the job with `SELECT … FOR SHARE`;
     - checks status, deadline (in `Asia/Kolkata`) and eligibility (`EligibilityPolicy`);
     - inserts the application plus its first timeline row.
   - The `UNIQUE (student_id, job_id)` key is the final judge: a duplicate becomes `409 APPLICATION_ALREADY_EXISTS`.
   - The frontend disables the button while the request is in flight and shows a 409 as "Applied".
3. **Data model and migrations (ER diagram).**
   - Nine tables, owned by Flyway `V1`–`V4`. Hibernate only validates the schema against the entities.
   - The baseline migration let me adopt Flyway on a live Azure database without recreating it.
   - Constraints carry the rules: unique keys, `CHECK` on status values and on slot capacity, `version` columns.
4. **The four concurrency scenarios** (table in the README, details in `docs/concurrency.md`). Each has a different, deliberately chosen tool:

   | Scenario | Tool |
   |---|---|
   | Duplicates | Unique key |
   | Job closing during apply | Shared row lock |
   | Seats | Atomic conditional `UPDATE` |
   | Status edits | Optimistic locking |

   Each is proven by a 20-thread test against MySQL and mutation-checked.
5. **Performance.**
   - N+1 fixed with entity graphs, batched timeline loading and `open-in-view: false`. `QueryCountIntegrationTest` pins "My Applications" at 4 SQL statements no matter how many rows.
   - The k6 baseline is about 2,200–2,400 search req/s on a laptop.
   - The connection-pool experiment showed pool 20 is faster only far above the expected load, so I kept 10. Measure first, then tune.
6. **Security.**
   - BCrypt passwords; HS256 JWTs with a secret of at least 32 bytes from configuration.
   - Role is re-read from the database on every request.
   - Another user's data returns 404, not 403.
   - Rate limits run before BCrypt and before Gemini.
   - nginx overwrites `X-Forwarded-For` so a client can't spoof its IP.
   - Actuator exposes only health publicly; metrics are admin-only.
7. **Delivery.**
   - Multi-stage, non-root Docker images; the Compose start order is gated on health checks.
   - GitHub Actions runs backend tests against a MySQL service container, frontend lint + tests + `npm audit` + build, and a Docker build with a Trivy scan.
   - Both Azure deploys `need` that CI to pass.
   - Rollback = redeploy the last good commit. Migrations are forward-only.
8. **Close with limitations and next steps** (see the end of this document). Interviewers trust a candidate who knows where the edges are.

---

## Interview questions

### 1. Why did you choose Spring Boot?

- **Fit for the domain.** This is a transactional business application with roles, rules and a relational database. Spring Boot gives mature, well-integrated building blocks for exactly that:
  - Spring Data JPA, with `@Transactional` and declarative locking (`@Lock`, `@Version`, `@Modifying` queries);
  - Spring Security filters and method security;
  - Bean Validation;
  - Actuator for health and metrics;
  - Flyway auto-configuration.
- **I wrote business logic, not plumbing.** For example, the readiness probe that includes the database is one line of config (`management.endpoint.health.group.readiness`). Graceful shutdown is `server.shutdown: graceful`.
- **Testability.** I use a range of test styles:
  - plain Mockito unit tests (`Milestone2ServiceRulesTest`);
  - MockMvc web-layer tests (`ApiSecurityTest`);
  - full-context integration tests against MySQL (`it/*`);
  - all of it runs in CI with `./mvnw verify`.
- **Trade-offs:** heavier startup and memory than a micro-framework, and "magic" that must be understood (open-session-in-view, lazy loading). I turned `open-in-view` off on purpose so lazy-loading mistakes fail loudly instead of quietly issuing queries.

### 2. How does JWT authentication work in your application?

1. **Login** (`AuthController` → `AuthService`): Spring's `AuthenticationManager` checks the password against the BCrypt hash. `JwtUtil.generateToken` signs an HS256 token with `sub = email`, a `role` claim, `iat` and `exp` (24 h by default).
2. **The secret** comes from configuration (`APP_JWT_SECRET`). `JwtUtil.init()` refuses to start the app if it's shorter than 32 bytes; this is tested in `shortSecretFailsFastAtStartup`.
3. **The client** stores the token. The Axios interceptor (`frontend/src/api/axiosInstance.js`) adds `Authorization: Bearer …` to every request.
4. **Every request:** `security/JwtFilter.java` verifies the signature and expiry, then **reloads the user from the database**. The role and active flag come from MySQL, not from the token, so deactivating someone works on their very next request. `ApiSecurityTest.roleClaimInTokenIsIgnoredInFavourOfDatabaseRole` proves a forged role claim is ignored.
5. **A bad token never throws out of the filter.** The request stays anonymous and the reason (expired / invalid / disabled) is recorded. Protected endpoints then get a JSON 401 from `SecurityErrorWriter`, and public endpoints such as login still work with a stale token in the browser.
6. **The frontend** reacts to a 401 from a protected endpoint by clearing the session and going to `/login`. A 401 from `/api/auth/*` (a wrong password) is shown on the form instead. That was a real bug I fixed, and it now has a regression test.
7. **Stateless:** `SessionCreationPolicy.STATELESS` and no cookies, so CSRF protection is off, and any instance can serve any request.

**Known trade-offs:**
- **No server-side revocation and no refresh token.** Logout deletes the token in the browser.
- **The token lives in `localStorage`, readable by an XSS attack.** It's mitigated by React escaping and security headers.
- **Planned improvement:** short access tokens plus an `HttpOnly` refresh cookie. That is now practical because the Docker/nginx setup is same-origin.

### 3. How do you prevent duplicate applications?

There are three layers, and **only the last one is the actual guarantee**:
1. **UI:** the Apply button is disabled while the request is in flight (tested in `JobDetails.test.jsx`, "double-click sends one request").
2. **Service:** `StudentService.applyToJob` calls `existsByStudentIdAndJobId`. That gives a fast, friendly answer in the normal case.
3. **Database:** `UNIQUE (student_id, job_id)` from `V3__application_integrity.sql`. Two simultaneous requests can both pass step 2, but MySQL rejects the second insert. I catch that specific constraint (`isConstraint(e, "uk_applications_student_job")`) and return the same `409 APPLICATION_ALREADY_EXISTS`.

The application row and its timeline row are inserted in one transaction, so a refused attempt leaves nothing behind.

**Proof:** `ConcurrencyIntegrationTest.simultaneousDuplicateApplicationsCreateExactlyOneRow` sends 20 threads → 1 success, 19 × 409, 1 row. **With the unique key dropped, the same test produced 12 duplicates.** That's how I know the race is real, not theoretical.

Under load (k6), 200 students × 3 simultaneous clicks left exactly 200 applications. Retrying "apply" is also effectively idempotent: a retry can never create a second row.

### 4. How do database transactions work?

- A transaction groups statements so that **all succeed or none do**, and isolates them from other transactions.
- In Spring, `@Transactional` on a service method opens a transaction on entry, commits on normal return, and **rolls back on a runtime exception**.
- **In this code:**
  - every service class is `@Transactional(readOnly = true)`, and each write method overrides it with `@Transactional`;
  - **Registration:** the user row and the student/recruiter row are written together. `TransactionRollbackIntegrationTest.failedProfileInsertRollsBackTheUserRow` proves a failed profile insert leaves no orphan user. It failed when I removed `@Transactional`.
  - **Apply:** application + timeline in one transaction.
  - **Booking:** the seat increment and the booking insert are in one transaction. If the insert fails, the seat is given back automatically.
  - **Status change:** the status update and its timeline row commit together.
- **Isolation:** MySQL InnoDB defaults to `REPEATABLE READ`. A plain `SELECT` reads a snapshot and takes no locks, which is exactly why "check then insert" is not safe on its own. It's also why I use constraints, `FOR SHARE` locks and atomic updates where a rule must hold under concurrency.
- **Scope:** `spring.jpa.open-in-view: false` keeps the database connection tied to the service transaction, not the whole HTTP request. Connections return to the 10-connection pool quickly.

### 5. What is optimistic locking?

- **Idea:** don't lock while reading; detect a conflict when writing.
- Each row has a `version` column. An update is `UPDATE … SET …, version = version + 1 WHERE id = ? AND version = ?`. If someone else changed the row first, the version no longer matches, 0 rows are updated, and the write fails.
- **In PlacementHub:** `applications.version` and `jobs.version` (migration V3, `@Version` in the entities).
- **It suits** conflicts that are rare and involve human think time, like two recruiters editing the same application minutes apart. Holding a database lock for minutes would be absurd.
- **Pessimistic locking** (`SELECT … FOR UPDATE / FOR SHARE`) blocks others until commit. I use a shared lock in exactly one place: reading the job during apply (question 6, scenario B).

### 6. What happens when two requests update the same record?

It depends on the record. I chose a different mechanism for each case:

- **Application status (two recruiters):**
  1. **Client version check.** Every response includes `version`, and the UI sends it back with the new status. If the record changed since the recruiter loaded it, they get `409 APPLICATION_MODIFIED`, "refresh and try again". This catches the common case: a stale screen.
  2. **`@Version`.** If two requests pass step 1 in the same millisecond, the second `UPDATE … WHERE version = ?` matches 0 rows. Hibernate throws an optimistic-lock exception, which `GlobalExceptionHandler` maps to `409 CONCURRENT_UPDATE`.
  3. **State machine.** `ApplicationStatus` only allows valid moves, for example `REJECTED → OFFERED` is `422`.
  - **Result:** no lost update and no duplicate timeline rows. `concurrentStatusUpdatesFromTheSameVersionLetExactlyOneWin`: 20 threads → 1 success, 19 conflicts, version + 1, one timeline row.
- **Interview slot counter (many students):** optimistic locking would make most of them fail even with seats left. Instead there's one atomic statement, `UPDATE interview_slots SET booked_count = booked_count + 1 WHERE id = ? AND booked_count < capacity` (`InterviewSlotRepository.reserveSeat`). MySQL row-locks during that statement, so check-and-increment is indivisible. 0 rows updated → `409 SLOT_FULL`. A `CHECK (booked_count <= capacity)` is the safety net. Test: 20 students, 3 seats → exactly 3 bookings.
- **Job being closed while students apply:** applying reads the job `FOR SHARE`. Closing needs an exclusive lock, so it waits for applications already in progress, and later applications see `CLOSED`. Test: `closingAJobWaitsForAnApplicationThatIsAlreadyInProgress`.

### 7. How does Docker Compose connect the frontend, backend, and database?

`compose.yaml` defines three services on one Compose network:
- **`mysql`** (`mysql:8.0.46`):
  - data lives in the named volume `mysql-data`, so it survives `docker compose down`;
  - its port is published only on `127.0.0.1:3307`, for local tools;
  - the health check runs `mysqladmin ping` over TCP.
- **`backend`:**
  - connects to `jdbc:mysql://mysql:3306/…`, where `mysql` is resolved by Docker's internal DNS;
  - secrets come from `.env` through environment variables (`${MYSQL_PASSWORD:?…}` fails fast if one is missing);
  - its health check is `/actuator/health/readiness`, which includes a database check;
  - it starts only when MySQL is healthy.
- **`frontend`:**
  - unprivileged nginx serving the built React app on port 3000;
  - it **proxies `/api` to `http://backend:8080`**, so the browser talks to one origin: no CORS and no backend URL compiled into the JavaScript;
  - nginx replaces `X-Forwarded-For` with the real client address, so rate limits can't be dodged;
  - it starts only when the backend is ready.

Start order is therefore *healthy*, not just *started*: database → migrated, reachable backend → frontend.

Images are multi-stage (build with the JDK or Node, run with the JRE or nginx) and run as non-root users; `.dockerignore` keeps local secrets out. Compose is my local and load-test environment. Production uses Azure managed services, not Compose.

### 8. How would you scale PlacementHub?

Measure first. The k6 baseline (one laptop) is about 2,200–2,400 search req/s, while a university placement season peak is estimated at about 150 req/s. Today the honest answer is "it doesn't need scaling yet". When it does, in order:

1. **Vertical and config.**
   - A bigger App Service plan, and the Hikari pool size, which is configurable without code changes.
   - My pool experiment showed connection wait was the first bottleneck at saturation.
2. **Horizontal app instances.**
   - This already works because the API is stateless (JWT, no HTTP session).
   - One change is needed first: **rate limits are in memory per instance**, so move them to a shared store (Redis).
   - Also watch MySQL `max_connections`: instances × pool size.
3. **Database.**
   - Indexes already cover the hot list queries (V2).
   - Next: a read replica for the read-heavy search and admin analytics, and keep transactions short.
   - The concurrency rules stay correct because they live in the primary database.
4. **Caching** only for measured hot reads, like approved job listings, with short TTLs.
5. **Async work.** Emails and notifications would go through a queue, so requests don't wait on them.

What I would *not* do: split into microservices or add Kafka. Nothing in the measured load or the team size calls for it.

### 9. When would you add Redis?

When there is a concrete need, not for appearance. The concrete triggers for this app:
- **More than one backend instance.** The token-bucket rate limiter (`ratelimit/RateLimiter.java`) keeps its counters in memory, so with N instances a client effectively gets N × the limit. Redis would hold shared counters. This is the first real reason.
- **Token revocation.** With a "log out everywhere" or refresh-token feature, a Redis denylist or refresh-token store with TTLs fits well.
- **A measured hot read** that indexes can't serve fast enough, for example the approved-jobs list during peak season, cached with a short TTL and invalidated on approve/close.

I would **not** use Redis for distributed locks here. Every invariant lives in one MySQL database, which already has transactions, constraints and row locks. A second locking system could disagree with the database and adds a failure mode.

### 10. Why choose a modular monolith over microservices?

- **The rules are cross-entity and transactional.** "Apply" touches jobs, students, applications and the timeline atomically. "Book" touches slots, bookings and applications. In one database these are local transactions and constraints. Across services they become sagas, compensation and eventual consistency, which is much harder to get right and to test.
- **Scale doesn't demand it.** Measured capacity is far above the expected load, and one stateless app can scale horizontally.
- **Team size.** A single developer (or small team) gets no organisational benefit from independent deployables, only operational cost: more pipelines, network failures and distributed tracing.
- **Modular, not a big ball of mud.**
  - Code is split by layer, and by role inside each layer (`StudentService`, `RecruiterService`, `AdminService`, `InterviewSlotService`).
  - Shared rules have one home (`EligibilityPolicy` is used by both apply and Recommended; shared mappers replaced 5 copy-pasted ones).
  - If one area ever needed independent scaling, the AI chat is the obvious first candidate: it's stateless and calls an external API.

### 11. How do you identify and fix N+1 queries?

- **The problem.** Load a list of N entities, then touch a lazy association on each one, and you get 1 + N queries. My audit found "My Applications" running one timeline query per application, plus eager loads.
- **Identify:**
  - Hibernate statistics or SQL logging in development;
  - the slow-query log (`LOG_QUERIES_SLOWER_THAN_MS: 200`);
  - most reliably, **a test that counts statements**: `it/QueryCountIntegrationTest` uses Hibernate `Statistics` and asserts an exact count.
- **Fix:**
  - all `@ManyToOne`/`@OneToOne` relations are `LAZY`, so nothing loads unless asked;
  - `@EntityGraph` fetch joins on list queries, e.g. `ApplicationRepository` loads `job` and `job.company` (plus `student.user` for recruiters) in the same query;
  - collections load in **one `IN` query for the whole page**: `ApplicationMapper` fetches all timelines and all bookings for the listed applications at once;
  - `default_batch_fetch_size: 50` as a safety net;
  - `open-in-view: false`, so lazy loading in a controller or serializer fails instead of silently querying.
- **Result, pinned by tests:**

  | Use case | SQL statements |
  |---|---|
  | My Applications (any number of applications) | 4 |
  | Approved jobs | 1 |
  | Recruiter's jobs | 2 |
  | Applicants for a job | ≤ 5 |
  | Admin student page | ≤ 2 |

  If someone adds a lazy access in a loop, the test fails in CI.

### 12. How would you deploy the application safely?

This is what the pipeline does today:
1. **Pull request.** `ci.yml` runs three jobs:
   - backend `./mvnw verify` against a MySQL 8.0.46 service container (unit, security, concurrency, migration and integration tests);
   - frontend lint, 59 component tests, `npm audit` (fails on critical) and the production build;
   - Docker build of both images plus a Trivy scan (fails on a fixable CRITICAL).
2. **Merge to `main`.** Both Azure deploy workflows `need: ci`, so a red check means nothing is deployed and the live site keeps the previous version. The deploy job's `-DskipTests` is safe only because CI already ran every test.
3. **On startup, Flyway applies pending migrations before the app takes traffic.** Migrations are forward-only and additive where possible. Before a release with a migration I take a backup (Azure point-in-time restore). Before adding `UNIQUE (student_id, job_id)` I first checked production for existing duplicates, because the migration would fail on them.
4. **Health and smoke test.** `/actuator/health/readiness` includes the database, and it is the App Service health-check path. Then a quick manual check of login and search.
5. **Rollback:**
   - re-run the deploy of the last good commit, or revert the merge commit;
   - a schema change is undone with a new migration, never by editing an applied one.
6. **Configuration and secrets:**
   - secrets are only environment variables or GitHub secrets;
   - nothing real is in git;
   - Docker images are checked to contain no `application.properties`.

**Gap I'd close next:** a staging slot with a swap, so each release is verified on Azure before it reaches users. Branch protection that *requires* the CI checks is a one-time repository setting.

### 13. How do you test race conditions?

- **The principle:** a race only shows when operations really overlap, so the test must create real concurrency against the **real database**. A mock or an H2 database doesn't have MySQL's locking behaviour.
- **How `it/ConcurrencyIntegrationTest` does it:**
  - It starts 20 threads that each wait on a **start gate** (a latch), then releases them at the same instant.
  - Each thread calls the real service in its own transaction, logged in as the right user.
  - The test asserts **both** the responses (e.g. exactly 1 × 201 and 19 × 409) **and the database state afterwards** (exactly one row, `booked_count = 3`, one timeline row).
  - The test pool is raised to 40 connections, so all 20 threads really hold a connection at once; with 10, they'd partly queue and the race would be weaker.
- **Deterministic ordering where timing matters.** For "close waits for an in-flight application", one transaction holds the job's shared lock for 1.5 s, and the test asserts that `closeJob` blocked for at least 1 s.
- **Mutation checks prove the tests can fail:**
  - with the unique key removed, the duplicate test saw 12 duplicates;
  - with the lock annotation removed, the close test failed.
  - A concurrency test that never fails is worthless.
- **Then under load:** the k6 `apply_burst` (200 students × 3 simultaneous clicks) and `slot_burst` (100 students, 20 seats) scenarios check the same invariants through HTTP, nginx and the connection pool, and assert zero 5xx responses.
- **Frontend:** component tests check that a double-click sends one request, and that a 409 is shown sensibly.

### 14. How do you prevent unauthorized access to another student's application?

There are two layers, because the URL rule alone isn't enough:
1. **Role check by URL** (`config/SecurityConfig.java`): `/api/students/**` requires `ROLE_STUDENT`, `/api/recruiter/**` requires `ROLE_RECRUITER`, `/api/admin/**` requires `ROLE_ADMIN`. The role comes from the database on every request, not from the token (question 2). A student calling an admin API gets 403 (`ApiSecurityTest.studentCallingAdminApiIs403NotBadRequest`).
2. **Ownership check in the service.** Every student can call `/api/students/applications/{id}`, so the check must compare the record's owner with the caller.
   - **The caller's identity comes from the verified token,** never from a request parameter: the student is looked up from the authenticated email (`SecurityUtils.getCurrentUserEmail()`). There is no `studentId` in the request to tamper with.
   - **`StudentService.requireOwnApplication`** loads the application and throws `NotFoundException("Application not found")` if `application.getStudent().getId()` isn't the caller's id. Booking slots and cancelling go through the same method.
   - **Recruiters:** `RecruiterService` does the same with `job.getRecruiter().getId()` for jobs, applicants, status changes and slots.
   - **404, not 403:** the response for "someone else's" is identical to "doesn't exist", so ids can't be probed to discover which records exist.
   - **Lists** are always queried by the caller's id (`findByStudentId(student.getId())`), so another student's data can't appear in them.
3. **Tests:**
   - `Milestone2ServiceRulesTest.anotherStudentsApplicationLooksLikeItDoesNotExist`: mutation-checked; it fails if the ownership check is removed;
   - `anotherRecruitersJobLooksLikeItDoesNotExist`;
   - the 401/403 cases in `ApiSecurityTest`.

---

## Numbers worth remembering

| Fact | Value | Source |
|---|---|---|
| Backend tests | 105 (unit, web security, MySQL integration, concurrency) | `./mvnw verify` in CI |
| Frontend tests | 59 component tests, ~57% line coverage | `npm test` |
| Duplicates without the unique key | 12 from 20 simultaneous clicks | concurrency mutation check |
| Search throughput (one laptop) | ~2,200–2,400 req/s at p95 40–45 ms | `docs/performance.md` |
| Apply burst | 200 students × 3 clicks → exactly 200 applications, 0 errors | k6 |
| Slot burst | 100 students, 20 seats → exactly 20 bookings | k6 |
| "My Applications" SQL statements | 4, for any number of applications | `QueryCountIntegrationTest` |
| Expected peak load | ~150 req/s (estimate) | audit §6.1 |

## Honest limitations (say these before you are asked)

- No automated browser E2E tests yet; component and integration tests only.
- Rate limits are per instance (in memory).
- JWT is in `localStorage`, with no refresh tokens or server-side revocation.
- Load numbers come from one laptop, not from Azure.
- No password reset; branch eligibility is free-text matching.
