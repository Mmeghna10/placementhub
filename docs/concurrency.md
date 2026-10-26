# Concurrency and data consistency in PlacementHub

This document covers the business rules that must hold **even when many requests arrive at the same moment**. For each one it gives the race, the mechanism that prevents it, why that mechanism was chosen over the alternatives, and the test that proves it against real MySQL.

All tests referred to here are in `backend/src/test/java/com/placementhub/backend/it/ConcurrencyIntegrationTest.java`. Each test releases 20 threads at the same instant through a start gate. Each thread calls the real service in its own transaction, and then the test checks both the responses and the database rows.

---

## Background: what actually happens with concurrent requests

| Concept | In PlacementHub |
|---|---|
| **Concurrent HTTP requests** | Each request runs on its own server thread with its own DB connection and transaction (Hikari pool, max 10 in production). Nothing makes them take turns unless the database does. |
| **Transaction isolation** | MySQL InnoDB defaults to `REPEATABLE READ`: a plain `SELECT` reads a snapshot and takes **no locks**. Two transactions can therefore both read "this student hasn't applied yet" and both insert. That's why a check in Java alone is not enough. |
| **Optimistic locking** | No lock while reading. The write is `UPDATE … SET version = version + 1 WHERE id = ? AND version = ?`. If someone else changed the row first, 0 rows match and the update fails with **409**. It suits rare conflicts with human "think time" between read and write. |
| **Pessimistic locking** | `SELECT … FOR SHARE` / `FOR UPDATE` locks the row until commit, so others wait. Correct, but it blocks, so use it only on short, hot paths. |
| **Atomic conditional update** | One SQL statement both checks and changes: `UPDATE … SET n = n + 1 WHERE n < capacity`. The row lock taken by the UPDATE makes the check-and-change indivisible. |
| **Idempotency** | Repeating a request has no extra effect. "Apply" is effectively idempotent: a retry gets **409 APPLICATION_ALREADY_EXISTS** and nothing new is created. |
| **Distributed locks** (Redis, ZooKeeper) | **Not used, and not needed.** Every invariant here lives in one MySQL database, which already provides locks and constraints. A distributed lock would add a second system that could disagree with the database. |

---

## Scenario A: duplicate applications

**The race:** a student double-clicks **Apply**, retries after a timeout, or applies from two tabs. Each request runs `exists(student, job)` → `false` → `INSERT`.

**Fix:**
- Migration `V3__application_integrity.sql` adds `UNIQUE (student_id, job_id)`.
- `StudentService.applyToJob` keeps the `exists()` check as a fast, friendly path, but the database has the final word. A duplicate-key error on the unique key is turned into the same **409 APPLICATION_ALREADY_EXISTS**.
- The application row and its first timeline row are written in one transaction, so a refused request leaves nothing behind.

**Why not a lock?** Locking would make every application to a popular job wait in line. A unique key only stops actual duplicates; different students never wait on each other.

**Evidence:**
- `simultaneousDuplicateApplicationsCreateExactlyOneRow`: 20 simultaneous applies by one student give exactly 1 success, 19 × 409, 1 application row and 1 timeline row.
- **Mutation check:** with the unique key temporarily dropped from the test database, the same test produced **12 duplicate rows from 20 clicks**. The race is real, and the Java check alone does not stop it.

---

## Scenario B: deadline, closing a job, and eligibility

**Rules, all checked on the server:**
- The job must be `APPROVED`.
- Today must be on or before `application_deadline`.
- The student must meet `min_cgpa` and `eligible_branches` (`EligibilityPolicy`, the same rules used by the "Recommended" list).
- The deadline compares **dates in `app.timezone`** (default `Asia/Kolkata`). Azure servers run in UTC, so `LocalDate.now()` without a zone would be wrong between 00:00 and 05:30 IST.

**The race:** a recruiter closes the job (or edits it, which sends it back to `PENDING`, or moves the deadline) while a student's application is half-way through its checks.

**Fix:** `applyToJob` reads the job with a **shared lock** (`SELECT … FOR SHARE`, via `JobRepository.findByIdForApplication`).
- Many applications can hold the shared lock at once, so applying never blocks applying.
- Closing, editing or re-reviewing the job `UPDATE`s the row, which needs an **exclusive** lock. It waits until in-flight applications commit.
- Applications that start after the change see the new state and are refused.

**Decision rule** (which event decides): an application is accepted if, at the moment its transaction reads the locked job row, the job is `APPROVED` and today (`app.timezone`) is not after the deadline. A job close that commits first wins; an application that read the job first wins.

**Why not optimistic locking on the job?** Applications don't modify the job row, so there is no version to conflict on. A plain `SELECT` could read a job that is being closed at that moment. The shared lock is the cheapest correct option here.

**Evidence:**
- `closingAJobWaitsForAnApplicationThatIsAlreadyInProgress`: while a transaction holds the job's shared lock for 1.5 s, `closeJob` blocks for at least 1 s. Afterwards a new application is refused with `JOB_NOT_OPEN`.
  - **Mutation check:** with the lock annotation removed, the close did not wait and the test failed.
- `applicationsRacingAJobClosureAreEitherFullyAcceptedOrRefused`: 20 applications race one close. Every request is either accepted or refused with `JOB_NOT_OPEN`, and the number of application rows equals the number accepted (no "phantom" applications).
- `deadlineAndEligibilityAreEnforcedOnTheServer`: covers yesterday's deadline (refused), today's deadline (accepted), a CGPA below the minimum (refused), and expired jobs being hidden from listings.

---

## Scenario C: limited interview slots (new feature)

**Feature:**
- A recruiter publishes interview slots for a job, each with a start time, duration, location and **capacity**.
- A student whose application is in `INTERVIEW` books one seat in one slot, and can cancel before it starts.

**The race:** 20 students click **Book** for the last 3 seats at the same instant.

**Fix (three layers, migration `V4__interview_slots.sql`):**
1. `InterviewSlotRepository.reserveSeat` is a single **atomic conditional UPDATE**: `booked_count = booked_count + 1 WHERE id = ? AND booked_count < capacity`. MySQL locks the row, re-checks and increments in one step. A result of 0 rows means **409 SLOT_FULL**.
2. `CHECK (booked_count <= capacity)` is a safety net, in case any future code bypasses the service.
3. `UNIQUE (application_id)` on `interview_bookings` allows one booking per application.

**Order inside `bookSlot`:**
1. Early checks.
2. Reserve the seat.
3. Insert the booking.

If step 3 fails (the same student raced two booking requests), the **whole transaction rolls back, including the seat** reserved in step 2, so seats never leak.

**Alternatives considered:**

| Option | Verdict |
|---|---|
| Read `booked_count` in Java, check, then save | ❌ Classic lost update: two requests both read 2/3 and both write 3/3. |
| `@Version` on the slot | Correct, but under a rush most requests fail with "try again" even when seats are left. |
| `SELECT … FOR UPDATE` on the slot | Correct, but it holds a lock across Java code. More moving parts than needed. |
| **Atomic conditional UPDATE** ✅ | Correct, one statement, and the lock is held only for that statement. Simplest correct option for a counter. |

**Evidence:**
- `aSlotIsNeverOverbookedEvenWhenStudentsBookAtTheSameInstant`: 20 students, 3 seats, giving exactly 3 bookings, `booked_count = 3` and 17 × `SLOT_FULL`.
- `oneStudentDoubleClickingTwoSlotsGetsOneBookingAndNoLeakedSeat`: 20 requests from one student across two slots give 1 booking, and the total seats taken across both slots is exactly 1.

---

## Scenario D: concurrent application status changes

**Rules:** `ApplicationStatus` is a state machine, and only these moves are allowed:
- `APPLIED → SHORTLISTED → INTERVIEW → OFFERED`;
- `APPLIED`, `SHORTLISTED` or `INTERVIEW` → `REJECTED`;
- `OFFERED` and `REJECTED` are final.

The API returns `allowedNextStatuses`, so the UI only offers valid moves.

**The race:** two recruiters (or two tabs) look at the same application. One clicks REJECTED and the other SHORTLISTED. Without protection, the last write silently wins and the timeline records both.

**Fix:**
1. **Client version:** each response includes `version`, and the UI sends it back. A different current version gives **409 APPLICATION_MODIFIED** ("refresh and try again").
2. **`@Version` column (V3):** if two transactions pass step 1 at the same instant, the second `UPDATE … WHERE version = ?` matches 0 rows, giving **409 CONCURRENT_UPDATE**.
3. **Transition check:** for example, `REJECTED → OFFERED` gives **422 INVALID_STATUS_TRANSITION**.

The status change and its timeline row commit together, so a refused update adds no timeline entry.

**Evidence:**
- `concurrentStatusUpdatesFromTheSameVersionLetExactlyOneWin`: 20 updates from the same version give exactly 1 success, 19 conflicts, `version + 1`, and exactly 1 timeline row.
- `invalidTransitionsAreRefused`: a `REJECTED` application cannot become `OFFERED`.
- `Milestone4RulesTest`: the full transition table.

---

## Scenario E: offers (Offer Desk)

**Rules:**
- A job has `openings`; pending + accepted offers never exceed it.
- A student holds at most **one pending offer**, and is placed (accepts) at most once.
- An offer ends exactly once: the student accepts or declines it, or it expires at its deadline.

**The races:**
- A recruiting team offers more candidates than there are openings, at the same instant.
- Several companies offer the same student at the same instant.
- The student accepts in one tab and declines in another, or the expiry job runs at the same moment.

**Fix (all in migration `V5__offer_desk.sql`):**
1. **Seats:** `JobRepository.reserveOfferSeat` is an atomic conditional `UPDATE … SET offers_pending = offers_pending + 1 WHERE … offers_pending + offers_accepted < openings`, the same pattern as interview seats, with a `CHECK` constraint underneath. 0 rows → `409 NO_OPENINGS_LEFT`.
2. **One pending offer:** `StudentRepository.claimPendingOffer` sets `pending_offer_application_id` only `WHERE … IS NULL AND placed_application_id IS NULL`. 0 rows → `409 STUDENT_HAS_PENDING_OFFER` (or `422 STUDENT_ALREADY_PLACED`). The seat taken in step 1 rolls back with it.
3. **One outcome:** `OfferService` changes the application **first** and flushes, so the application's `@Version` picks the single winner before any counter is touched. The loser rolls back as a whole.
4. **Expiry:** `OfferExpiryJob` runs every minute and expires each overdue offer in its own short transaction. Because of step 3 it is safe to run on several servers at once, **without a distributed lock**.

**Evidence** (`OfferDeskIntegrationTest`, 20 threads, real MySQL):
- `openingsAreNeverOverOfferedWhenRecruitersOfferAtTheSameInstant`: 3 openings, 20 offers → exactly 3 offers, 17 × `NO_OPENINGS_LEFT`, counters 3/0.
- `aStudentHoldsOnlyOnePendingOfferEvenWhenCompaniesOfferTogether`: 20 companies → 1 offer; the other 19 jobs keep their seats.
- `anOfferIsAcceptedExactlyOnceEvenWhenTheStudentDoubleClicks`, `acceptAndDeclineAtTheSameInstantEndTheOfferExactlyOnce`, `expiryRacingADeclineEndsTheOfferOnceAndFreesTheOpening`: exactly one outcome, one timeline row, and counters that match it.

---

## Scenario F: overlapping interviews

**Rule:** a student cannot be in two interviews at once, even with different companies.

**Fix:** `bookSlot` locks the student's row, then checks the student's other bookings for an overlap (`409 INTERVIEW_CLASH`). One student's bookings therefore run one after another.

**A subtle bug the test found:** the first version took the lock with JPA's `PESSIMISTIC_WRITE`, and two overlapping bookings still both succeeded.
- **Why:** Hibernate generated `SELECT … FROM students JOIN users … FOR UPDATE OF students`. That locks the students row, but reads the joined `users` row with a plain, non-locking read. In MySQL's `REPEATABLE READ`, the first plain read fixes the transaction's snapshot, so the snapshot was taken *before* waiting for the lock. After the wait, the overlap check looked at the old snapshot and could not see the booking the other request had just committed.
- **Fix:** `StudentRepository.lockByUserEmail` is native SQL with a plain `FOR UPDATE` (it locks both rows, with no plain read), and it is the transaction's first statement.
- **Evidence:** `aStudentIsNeverBookedIntoTwoOverlappingInterviews`, two tabs booking two companies' overlapping slots → exactly 1 booking, no seat leaked. It failed (2 bookings) before the fix.

---

## Running the tests

```powershell
$env:PLACEMENTHUB_IT_DB_URL = "jdbc:mysql://localhost:3306/placementhub_it"
$env:PLACEMENTHUB_IT_DB_USER = "root"
$env:PLACEMENTHUB_IT_DB_PASSWORD = "YOUR_MYSQL_PASSWORD"
.\mvnw.cmd test -Dtest=ConcurrencyIntegrationTest
```

The integration tests raise the connection pool to 40, so all 20 threads really hold a connection at the same time. With the production pool of 10, they would partly queue and the race would be weaker.
