package com.placementhub.backend.config;

/**
 * Example requests and responses shown in Swagger UI (/swagger-ui.html) for the most
 * important endpoints, including their failure cases. Kept in one place so the controllers
 * stay readable. The values are illustrative, not real data.
 */
public final class ApiExamples {

    private ApiExamples() {
    }

    public static final String REGISTER_REQUEST = """
            {"fullName": "Asha Rao", "email": "asha@college.edu", "password": "s3cure-pass", "role": "STUDENT"}""";

    public static final String LOGIN_REQUEST = """
            {"email": "asha@college.edu", "password": "s3cure-pass"}""";

    public static final String AUTH_RESPONSE = """
            {"token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhc2hhQGNvbGxlZ2UuZWR1Ii...", "email": "asha@college.edu", "role": "STUDENT"}""";

    public static final String STATUS_UPDATE_REQUEST = """
            {"status": "SHORTLISTED", "remarks": "Strong Java skills", "version": 0}""";

    public static final String APPLICATION_RESPONSE = """
            {"id": 42, "jobId": 7, "jobTitle": "Backend Engineer", "companyName": "Acme", "status": "APPLIED",
             "appliedAt": "2026-10-24T10:15:00", "version": 0, "allowedNextStatuses": ["SHORTLISTED", "REJECTED"],
             "timeline": [{"status": "APPLIED", "remarks": "Application submitted", "changedAt": "2026-10-24T10:15:00"}]}""";

    public static final String VALIDATION_FAILED = """
            {"error": "Email must be valid", "status": 400, "code": "VALIDATION_FAILED", "path": "/api/auth/register",
             "timestamp": "2026-10-24T10:15:00Z", "requestId": "3f2c9a1e-...",
             "fieldErrors": {"email": "Email must be valid", "password": "Password must be between 6 and 72 characters"}}""";

    public static final String EMAIL_ALREADY_REGISTERED = """
            {"error": "Email already registered", "status": 409, "code": "EMAIL_ALREADY_REGISTERED",
             "path": "/api/auth/register", "timestamp": "2026-10-24T10:15:00Z", "requestId": "3f2c9a1e-..."}""";

    public static final String INVALID_CREDENTIALS = """
            {"error": "Invalid email or password", "status": 401, "code": "INVALID_CREDENTIALS",
             "path": "/api/auth/login", "timestamp": "2026-10-24T10:15:00Z", "requestId": "3f2c9a1e-..."}""";

    public static final String RATE_LIMITED = """
            {"error": "Too many requests. Please wait 42 seconds and try again.", "status": 429, "code": "RATE_LIMITED",
             "path": "/api/auth/login", "timestamp": "2026-10-24T10:15:00Z", "requestId": "3f2c9a1e-..."}""";

    public static final String ALREADY_APPLIED = """
            {"error": "You have already applied to this job", "status": 409, "code": "APPLICATION_ALREADY_EXISTS",
             "path": "/api/students/jobs/7/apply", "timestamp": "2026-10-24T10:15:00Z", "requestId": "3f2c9a1e-..."}""";

    public static final String NOT_ELIGIBLE = """
            {"error": "This job requires a minimum CGPA of 8.50.", "status": 422, "code": "NOT_ELIGIBLE",
             "path": "/api/students/jobs/7/apply", "timestamp": "2026-10-24T10:15:00Z", "requestId": "3f2c9a1e-..."}""";

    public static final String DEADLINE_PASSED = """
            {"error": "The application deadline for this job (2026-10-20) has passed", "status": 422, "code": "DEADLINE_PASSED",
             "path": "/api/students/jobs/7/apply", "timestamp": "2026-10-24T10:15:00Z", "requestId": "3f2c9a1e-..."}""";

    public static final String NOT_FOUND = """
            {"error": "Application not found", "status": 404, "code": "NOT_FOUND",
             "path": "/api/students/applications/42", "timestamp": "2026-10-24T10:15:00Z", "requestId": "3f2c9a1e-..."}""";

    public static final String APPLICATION_MODIFIED = """
            {"error": "This application was updated by someone else. Refresh and try again.", "status": 409,
             "code": "APPLICATION_MODIFIED", "path": "/api/recruiter/applications/42/status",
             "timestamp": "2026-10-24T10:15:00Z", "requestId": "3f2c9a1e-..."}""";

    public static final String INVALID_STATUS_TRANSITION = """
            {"error": "An application cannot move from REJECTED to OFFERED (REJECTED is final)", "status": 422,
             "code": "INVALID_STATUS_TRANSITION", "path": "/api/recruiter/applications/42/status",
             "timestamp": "2026-10-24T10:15:00Z", "requestId": "3f2c9a1e-..."}""";

    public static final String SLOT_FULL = """
            {"error": "This interview slot is already full. Please pick another one.", "status": 409, "code": "SLOT_FULL",
             "path": "/api/students/applications/42/slots/3/booking", "timestamp": "2026-10-24T10:15:00Z", "requestId": "3f2c9a1e-..."}""";
}
