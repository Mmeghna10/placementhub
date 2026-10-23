// PlacementHub load test (k6). Three scenarios run one after another:
//
//   browse      50 students search and open jobs for a minute, with think time.
//               Measures p50/p95/p99 latency of the read path under realistic use.
//   search_throughput  the same 50 users searching back-to-back with no pauses for 30 s:
//               the request rate the stack sustains, and latency at that rate.
//   apply_burst APPLY_STUDENTS students apply to the SAME job at the same instant, each
//               sending 3 requests at once (double/triple click). Exactly one application
//               per student must exist afterwards; duplicates must be 409, never 5xx.
//   slot_burst  SLOT_STUDENTS students in the INTERVIEW stage book one slot with
//               SLOT_CAPACITY seats at the same instant. Exactly SLOT_CAPACITY bookings.
//
// Runs against an isolated stack (load-tests/run-load-test.ps1) through nginx, like a
// browser would. Rate limiting is switched off for that stack only: the test measures the
// application, and hundreds of sign-ups from one IP would otherwise be refused by design.

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Counter } from 'k6/metrics';
import exec from 'k6/execution';

const BASE = __ENV.BASE_URL || 'http://frontend:8080';
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL;
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;
const BROWSE_VUS = Number(__ENV.BROWSE_VUS || 50);
const APPLY_STUDENTS = Number(__ENV.APPLY_STUDENTS || 200);
const SLOT_STUDENTS = Number(__ENV.SLOT_STUDENTS || 100);
const SLOT_CAPACITY = Number(__ENV.SLOT_CAPACITY || 20);
const RUN = `${Date.now()}`;

const serverErrors = new Counter('server_errors');
const applyCreated = new Counter('apply_created');
const applyDuplicateRefused = new Counter('apply_duplicate_refused');
const slotBooked = new Counter('slot_booked');
const slotFullRefused = new Counter('slot_full_refused');
const invariantViolations = new Counter('invariant_violations');

export const options = {
    setupTimeout: '15m',
    teardownTimeout: '2m',
    scenarios: {
        browse: {
            executor: 'ramping-vus', exec: 'browse', startVUs: 0,
            stages: [{ duration: '15s', target: BROWSE_VUS }, { duration: '60s', target: BROWSE_VUS }, { duration: '5s', target: 0 }],
            gracefulRampDown: '5s',
        },
        // No think time: how many search requests per second the stack can serve.
        search_throughput: { executor: 'constant-vus', exec: 'searchNoThinkTime', vus: BROWSE_VUS, duration: '30s', startTime: '85s' },
        apply_burst: { executor: 'per-vu-iterations', exec: 'applyBurst', vus: APPLY_STUDENTS, iterations: 1, startTime: '120s', maxDuration: '60s' },
        slot_burst: { executor: 'per-vu-iterations', exec: 'slotBurst', vus: SLOT_STUDENTS, iterations: 1, startTime: '185s', maxDuration: '60s' },
    },
    thresholds: {
        'http_req_failed{scenario:browse}': ['rate<0.01'],
        'http_req_duration{scenario:browse}': ['p(95)<500'],
        'http_req_failed{scenario:search_throughput}': ['rate<0.01'],
        'http_req_duration{scenario:search_throughput}': ['p(95)<500'],
        // No limit, only reported: how long requests took while hundreds arrived at once.
        'http_req_duration{scenario:apply_burst}': ['max>=0'],
        'http_req_duration{scenario:slot_burst}': ['max>=0'],
        server_errors: ['count==0'],
        invariant_violations: ['count==0'],
        apply_created: [`count==${APPLY_STUDENTS}`],
        apply_duplicate_refused: [`count==${APPLY_STUDENTS * 2}`],
        slot_booked: [`count==${SLOT_CAPACITY}`],
        slot_full_refused: [`count==${SLOT_STUDENTS - SLOT_CAPACITY}`],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// ------------------------------------------------------------------ helpers

const json = (token) => ({ headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) } });

function must(res, expected, what) {
    if (res.status !== expected) fail(`${what}: expected ${expected}, got ${res.status} ${res.body}`);
    return res.json();
}

function countServerError(res) {
    if (res.status >= 500 || res.status === 0) serverErrors.add(1);
}

// Register many users in parallel batches (register returns a token directly).
function registerMany(prefix, count, role) {
    const tokens = [];
    for (let start = 0; start < count; start += 25) {
        const batch = [];
        for (let i = start; i < Math.min(count, start + 25); i++) {
            batch.push(['POST', `${BASE}/api/auth/register`, JSON.stringify({
                email: `${prefix}-${i}-${RUN}@load.test`, password: 'load-test-password', role, fullName: `${prefix} ${i}`,
            }), json()]);
        }
        for (const res of http.batch(batch)) tokens.push(must(res, 201, `register ${prefix}`).token);
    }
    return tokens;
}

function inBatches(requests, size = 25) {
    const out = [];
    for (let i = 0; i < requests.length; i += size) out.push(...http.batch(requests.slice(i, i + size)));
    return out;
}

// ------------------------------------------------------------------ setup

export function setup() {
    const admin = must(http.post(`${BASE}/api/auth/login`, JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASSWORD }), json()), 200, 'admin login').token;
    const recruiter = registerMany('recruiter', 1, 'RECRUITER')[0];
    must(http.put(`${BASE}/api/recruiter/profile`, JSON.stringify({ fullName: 'Load Recruiter', companyName: `LoadCo ${RUN}` }), json(recruiter)), 200, 'company');

    const deadline = new Date(Date.now() + 30 * 864e5).toISOString().slice(0, 10);
    const titles = ['Java Backend Engineer', 'React Developer', 'Data Analyst', 'DevOps Intern', 'QA Engineer', 'Android Developer',
        'ML Intern', 'Cloud Associate', 'Security Analyst', 'Product Intern'];
    const createJob = (title, type) => {
        const job = must(http.post(`${BASE}/api/recruiter/jobs`, JSON.stringify({
            title, description: `${title} at LoadCo`, jobType: type, location: ['Pune', 'Remote', 'Mumbai'][title.length % 3],
            salaryMin: 20000, salaryMax: 20000 + title.length * 1000, requiredSkills: 'Java, SQL', applicationDeadline: deadline,
        }), json(recruiter)), 201, 'create job');
        must(http.put(`${BASE}/api/admin/jobs/${job.id}/approve`, JSON.stringify({ status: 'APPROVED' }), json(admin)), 200, 'approve job');
        return job.id;
    };
    const browseJobIds = [];
    for (let i = 0; i < 30; i++) browseJobIds.push(createJob(`${titles[i % titles.length]} ${i}`, i % 3 === 0 ? 'INTERNSHIP' : 'FULL_TIME'));
    const applyJobId = createJob('Burst Apply Role', 'FULL_TIME');
    const slotJobId = createJob('Burst Interview Role', 'FULL_TIME');

    const applyTokens = registerMany('apply', APPLY_STUDENTS, 'STUDENT');
    const slotTokens = registerMany('slot', SLOT_STUDENTS, 'STUDENT');

    // Slot students apply, then the recruiter moves each application to INTERVIEW.
    const applications = inBatches(slotTokens.map((t) => ['POST', `${BASE}/api/students/jobs/${slotJobId}/apply`, null, json(t)]))
        .map((res) => must(res, 201, 'slot student apply'));
    for (const status of ['SHORTLISTED', 'INTERVIEW']) {
        inBatches(applications.map((a) => ['PUT', `${BASE}/api/recruiter/applications/${a.id}/status`,
            JSON.stringify({ status }), json(recruiter)])).forEach((res) => must(res, 200, `move to ${status}`));
    }
    const startsAt = new Date(Date.now() + 2 * 864e5).toISOString().slice(0, 16);
    const slot = must(http.post(`${BASE}/api/recruiter/jobs/${slotJobId}/slots`, JSON.stringify({
        startsAt, durationMinutes: 30, capacity: SLOT_CAPACITY, location: 'Room 1',
    }), json(recruiter)), 201, 'create slot');

    return {
        admin, recruiter, browseJobIds, applyJobId, slotJobId, slotId: slot.id, applyTokens,
        slotStudents: slotTokens.map((token, i) => ({ token, applicationId: applications[i].id })),
    };
}

// ------------------------------------------------------------------ scenarios

const QUERIES = ['', 'java', 'intern', 'react', 'data', 'pune', 'remote', 'engineer', 'sql'];

export function browse(data) {
    const token = data.applyTokens[__VU % data.applyTokens.length];
    const q = QUERIES[Math.floor(Math.random() * QUERIES.length)];
    const sort = ['newest', 'deadline', 'salary'][Math.floor(Math.random() * 3)];
    const page = Math.floor(Math.random() * 3);
    const res = http.get(`${BASE}/api/students/jobs/search?q=${q}&sort=${sort}&page=${page}&size=10`,
        { ...json(token), tags: { name: 'GET /jobs/search' } });
    countServerError(res);
    check(res, { 'search 200': (r) => r.status === 200 });

    if (Math.random() < 0.4) {
        const id = data.browseJobIds[Math.floor(Math.random() * data.browseJobIds.length)];
        const detail = http.get(`${BASE}/api/students/jobs/${id}`, { ...json(token), tags: { name: 'GET /jobs/{id}' } });
        countServerError(detail);
        check(detail, { 'job 200': (r) => r.status === 200 });
    }
    sleep(0.5 + Math.random()); // think time
}

export function searchNoThinkTime(data) {
    const token = data.applyTokens[exec.vu.idInTest % data.applyTokens.length];
    const q = QUERIES[Math.floor(Math.random() * QUERIES.length)];
    const res = http.get(`${BASE}/api/students/jobs/search?q=${q}&page=0&size=10`, { ...json(token), tags: { name: 'GET /jobs/search' } });
    countServerError(res);
    check(res, { 'search 200': (r) => r.status === 200 });
}

export function applyBurst(data) {
    // __VU is numbered across all scenarios; this counter is 0..APPLY_STUDENTS-1 here.
    const token = data.applyTokens[exec.scenario.iterationInTest];
    const params = { ...json(token), tags: { name: 'POST /jobs/{id}/apply' }, responseCallback: http.expectedStatuses(201, 409) };
    const url = `${BASE}/api/students/jobs/${data.applyJobId}/apply`;
    // Three simultaneous clicks from the same student.
    const responses = http.batch([['POST', url, null, params], ['POST', url, null, params], ['POST', url, null, params]]);
    for (const res of responses) {
        countServerError(res);
        if (res.status === 201) applyCreated.add(1);
        else if (res.status === 409 && res.json('code') === 'APPLICATION_ALREADY_EXISTS') applyDuplicateRefused.add(1);
    }
    if (responses.filter((r) => r.status === 201).length !== 1) invariantViolations.add(1);
}

export function slotBurst(data) {
    const student = data.slotStudents[exec.scenario.iterationInTest];
    const res = http.post(`${BASE}/api/students/applications/${student.applicationId}/slots/${data.slotId}/booking`, null,
        { ...json(student.token), tags: { name: 'POST /booking' }, responseCallback: http.expectedStatuses(201, 409) });
    countServerError(res);
    if (res.status === 201) slotBooked.add(1);
    else if (res.status === 409 && res.json('code') === 'SLOT_FULL') slotFullRefused.add(1);
}

// ------------------------------------------------------------------ verify the database state

export function teardown(data) {
    const applicants = must(http.get(`${BASE}/api/recruiter/jobs/${data.applyJobId}/applications`, json(data.recruiter)), 200, 'applicants');
    const slots = must(http.get(`${BASE}/api/recruiter/jobs/${data.slotJobId}/slots`, json(data.recruiter)), 200, 'slots');
    const slot = slots.find((s) => s.id === data.slotId);
    const ok = check(null, {
        [`exactly ${APPLY_STUDENTS} applications stored`]: () => applicants.length === APPLY_STUDENTS,
        [`slot booked_count == ${SLOT_CAPACITY}`]: () => slot.bookedCount === SLOT_CAPACITY,
        [`exactly ${SLOT_CAPACITY} bookings stored`]: () => slot.bookings.length === SLOT_CAPACITY,
    });
    if (!ok) invariantViolations.add(1);
    console.log(`DB check: applications=${applicants.length} (expected ${APPLY_STUDENTS}), slot booked=${slot.bookedCount}/${slot.capacity}, bookings=${slot.bookings.length}`);

    // Server-side view of the same run (admin-only metrics).
    const metric = (name, tag) => {
        const res = http.get(`${BASE}/actuator/metrics/${name}${tag ? `?tag=${encodeURIComponent(tag)}` : ''}`, json(data.admin));
        const measurements = res.status === 200 && String(res.headers['Content-Type']).includes('json') ? res.json('measurements') : null;
        if (!measurements) return `n/a (HTTP ${res.status})`;
        return measurements.map((m) => `${m.statistic}=${Number(m.value).toFixed(4)}`).join(' ');
    };
    console.log(`server http.server.requests (search): ${metric('http.server.requests', 'uri:/api/students/jobs/search')}`);
    console.log(`server http.server.requests (apply): ${metric('http.server.requests', 'uri:/api/students/jobs/{jobId}/apply')}`);
    console.log(`server hikaricp.connections.acquire (wait for a DB connection, seconds): ${metric('hikaricp.connections.acquire')}`);
    console.log(`server hikaricp.connections.usage (time a connection was held, seconds): ${metric('hikaricp.connections.usage')}`);
    console.log(`server hikaricp.connections.timeout (requests that never got a connection): ${metric('hikaricp.connections.timeout')}`);
}
