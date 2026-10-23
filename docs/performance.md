# Performance and load testing

The numbers below were measured, not estimated. They come from one Windows laptop running everything at once (MySQL, backend, nginx and the k6 load generator) in Docker Desktop, so they are **conservative and local**. They are not Azure production figures, and they don't claim the app handles "thousands of users". They show how the code behaves under load and whether it stays **correct** while doing so.

## How to run it

```powershell
powershell -ExecutionPolicy Bypass -File load-tests\run-load-test.ps1
# options: -PoolSize 20  -BrowseVus 50  -ApplyStudents 200  -SlotStudents 100  -SlotCapacity 20  -KeepStack
```

The script:
1. starts a **separate** Docker Compose project (`placementhub-loadtest`, its own database, port 3100) with one-off random secrets;
2. runs [`load-tests/k6/placementhub.js`](../load-tests/k6/placementhub.js) with `grafana/k6:1.8.1` on that project's network. Requests go **through nginx**, the same path a browser uses;
3. deletes the test stack and its database.

Your normal `placementhub` stack is never touched. Rate limiting is turned off **for the test stack only**: hundreds of test sign-ups come from one IP and would otherwise be refused, by design. Rate limiting has its own tests.

## What the test does

| Scenario | Load | Pass criteria (k6 thresholds) |
|---|---|---|
| `browse` | Ramp to 50 students who search jobs and open job pages, with 0.5–1.5 s think time, for 75 s | p95 < 500 ms, < 1% errors |
| `search_throughput` | The same 50 users searching back-to-back with **no pauses** for 30 s | p95 < 500 ms, < 1% errors |
| `apply_burst` | **200 students apply to the same job at the same instant, each sending 3 requests at once** | exactly 200 × 201 and 400 × 409 `APPLICATION_ALREADY_EXISTS`, **0** server errors |
| `slot_burst` | **100 students book one interview slot with 20 seats at the same instant** | exactly 20 × 201 and 80 × 409 `SLOT_FULL` |
| teardown | Reads the database back through the API | 200 applications stored, slot shows `booked_count = 20`, 20 bookings |

Setup creates the data through the real API: a recruiter, 32 approved jobs, 300 students, and 100 applications moved to `INTERVIEW`.

## Environment
- **Laptop:** Intel Core i7-12650H (10 cores / 16 threads), Windows 11, Docker Desktop with WSL 2 (16 CPUs, 7.6 GiB available to Docker).
- **Images:** `mysql:8.0.46`, backend on `eclipse-temurin:17.0.20_8-jre`, nginx `1.28.0`.
- **App defaults:** Hikari pool 10, Hibernate batch fetch 50, OSIV off.
- **Load generator:** k6 runs on the same machine and competes with the app for CPU.

## Results (2026-10-23)

### Baseline: connection pool 10 (the shipped default)

| Metric | Run A | Run B |
|---|---|---|
| `browse` latency p50 / p95 / p99 | 4.3 / 6.6 / 8.7 ms | 4.9 / 7.6 / 9.9 ms |
| `search_throughput` requests per second | **~2,410 req/s** | **~2,200 req/s** |
| `search_throughput` latency p50 / p95 / p99 | 19 / 40 / 52 ms | 21 / 45 / 61 ms |
| `apply_burst` latency median / p95 / max | not reported in this run | 485 ms / 1.07 s / 1.23 s |
| `slot_burst` latency median / p95 / max | not reported in this run | 306 / 337 / 341 ms |
| Failed requests (browse + throughput) | 0 of 77,298 | 0 of 70,850 |
| Server errors (5xx) | **0** | **0** |
| Correctness (200 applications, 20/20 seats) | ✅ | ✅ |

Server-side view (`/actuator/metrics`, admin-only), run B:

| Hikari metric | Value |
|---|---|
| Average wait to get a connection (`acquire`) | **7.7 ms** (max 426 ms) |
| Average time a connection was held (`usage`) | 1.9 ms (max 154 ms) |
| Requests that never got a connection (`timeout`) | 0 |

**Reading:** at saturation, a request waited about **4× longer for a database connection than it actually used one**. The pool, not MySQL and not the code, was the bottleneck under maximum load.

### Experiment: connection pool 20

| Metric | Pool 10 (runs A/B) | Pool 20 |
|---|---|---|
| Throughput | 2,200–2,410 req/s | **~2,650 req/s** |
| Throughput p95 / p99 | 40–45 / 52–61 ms | **32 / 42 ms** |
| Average wait for a connection | 7.7 ms | **2.3 ms** |
| Average connection use | 1.9 ms | 3.1 ms (MySQL busier, so each query is slower) |
| `apply_burst` median / max | 485 ms / 1.23 s | **269 ms / 1.14 s** |
| `slot_burst` median / max | 306 / 341 ms | 319 / 350 ms (no change) |
| Correctness | ✅ | ✅ |

**Decision: keep 10.** Pool 20 is about 10–20% faster **only at 2,000+ req/s**. The design assumption for a university's placement season is a peak of about 150 req/s (see the audit, §6.1), and at that rate both pools answer in single-digit milliseconds. A bigger pool also means more connections held open on the managed Azure MySQL server per instance. The size can be changed without a code change (`SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE`) if real metrics ever show connection waits. This is the "measure first, then optimize" rule applied, rather than tuning for traffic that doesn't exist.

## What the results show
- **Correctness holds under contention.** In every run: 200 simultaneous applicants × 3 clicks left exactly 200 applications; 100 students racing for 20 seats left exactly 20 bookings; and none of the hundreds of refused requests became a 500. These are the guarantees from [concurrency.md](concurrency.md), observed under real load and not only in integration tests.
- **The read path is cheap.** Search is 2 SQL statements (a page plus a count, see `JobSearchIntegrationTest`), and with think time it stays under 10 ms at p99.
- **Bursts are slower but bounded.** A burst of 600 apply requests arriving in the same instant queues for 10 connections and gets a share lock on the job row, so its p95 is about 1 s, with no timeouts and no errors.

## Limitations of these numbers
- The load generator, proxy, app and database share one laptop CPU, so a real deployment with a separate database will behave differently. These results are for comparing changes, not for promising capacity.
- Azure App Service (Basic tier) and Azure MySQL Flexible Server haven't been load-tested, and doing so against the live site would affect real users. A staging copy would be needed.
- Runs vary by about ±10% (see runs A and B), so differences smaller than that aren't meaningful.

## Observability added in Milestone 7
| What | Where |
|---|---|
| Correlation id on every request | `X-Request-Id` response header, `requestId` in JSON error bodies, `[requestId]` prefix on every log line (`RequestIdFilter`). A safe incoming `X-Request-Id` is kept; anything else is replaced. |
| Access log | One line per request: method, path, status, duration (logger `http.access`). Health probes and query strings are left out. |
| Structured logs | In Docker (`docker` profile), logs are JSON in Elastic Common Schema, including `requestId`. |
| Metrics | `GET /actuator/metrics` (**ADMIN only**): `http.server.requests` with p50/p95/p99 (`.percentile`), `hikaricp.connections.*`, JVM memory, threads and GC. |
| Slow queries | Any SQL statement slower than 200 ms is logged on `org.hibernate.SQL_SLOW`. |
| Health | `/actuator/health/liveness` and `/readiness` (public, no details), from Milestone 6. |

Example: find everything that happened during one failed request.
```powershell
docker compose logs backend | Select-String "<requestId from the error response>"
```
