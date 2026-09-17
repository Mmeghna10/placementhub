# PlacementHub

A full-stack campus placement portal connecting students, recruiters, and placement cells in one place. Students browse and apply to jobs, recruiters post roles and manage applicants, and admins approve postings and track activity — all backed by a real JWT-secured API and an AI-powered in-app guide.

> Built as a full-stack learning project — Spring Boot backend, React frontend, MySQL database, and a Gemini-powered chatbot.

---

## Features

### Public
- Landing page with role-aware navigation (shows "Sign In / Get Started" or "Go to Dashboard" depending on login state)
- Registration with role selection (Student / Recruiter)
- JWT-based login

### Student
- View and edit profile (college, branch, CGPA, skills, resume/LinkedIn/GitHub links)
- Browse all approved jobs, or filter to **Recommended** jobs matched by CGPA and branch
- View full job details and apply in one click
- Track every application with a dated status timeline (Applied → Shortlisted → Interview → Offered/Rejected)

### Recruiter
- Separate personal profile and company profile pages
- Post, edit, and delete job listings
- View applicants per job and update their status
- New jobs start as `PENDING` until admin approval

### Admin
- Dashboard with live counts (students, recruiters, companies, jobs, pending approvals, applications)
- Approve or reject pending job postings
- Browse all registered students and recruiters
- Analytics: applications-by-status bar chart, job-approval-breakdown pie chart

### In-App AI Guide
- Floating chatbot on every dashboard, powered by Google's Gemini API (free tier)
- Role-aware system prompt — answers reflect exactly what that role can do in the app
- No hardcoded FAQ — genuinely understands and answers open-ended questions

---

## Tech Stack

**Backend**
- Java 17, Spring Boot 4
- Spring Web, Spring Data JPA (Hibernate), Spring Security
- JWT (jjwt) for stateless authentication, BCrypt for password hashing
- MySQL 8
- Maven

**Frontend**
- React 19 + Vite
- React Router for client-side routing
- Tailwind CSS with a custom design system (color tokens, Fraunces/Inter/IBM Plex Mono typography)
- Axios with interceptors for JWT attachment and 401 handling
- Recharts for analytics visualizations
- Lucide React for icons

**AI**
- Google Gemini API (`gemini-3.6-flash`) called server-side, so the API key is never exposed to the browser

---

## Project Structure

```
placementhub/
├── backend/
│   └── src/main/java/com/placementhub/backend/
│       ├── config/        # Security, CORS, RestTemplate/ObjectMapper beans
│       ├── controller/    # REST endpoints
│       ├── service/       # Business logic
│       ├── entity/        # JPA entities
│       ├── repository/    # Spring Data JPA repositories
│       ├── dto/           # Request/response shapes
│       ├── security/      # JWT filter, JWT util, UserDetailsService
│       ├── exception/     # Global exception handling
│       └── enums/         # Role enum
└── frontend/
    └── src/
        ├── api/            # Axios calls, grouped by domain
        ├── components/     # Navbar, DashboardLayout, ChatWidget, shared UI
        ├── context/        # AuthContext
        ├── pages/
        │   ├── public/     # Landing, Login, Register
        │   ├── student/
        │   ├── recruiter/
        │   └── admin/
        ├── routes/         # ProtectedRoute
        └── utils/          # Misc helpers
```

---

## Running Locally

### Prerequisites
- JDK 17+
- Node.js 18+
- MySQL 8

### 1. Database
```sql
CREATE DATABASE placementhub_db;
```
Then run the schema (see `/backend/schema.sql` if included, or the table definitions in the entity classes).

### 2. Backend
Create `backend/src/main/resources/application.properties` (not committed — see `.gitignore`):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/placementhub_db
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

app.jwt.secret=YOUR_JWT_SECRET
app.jwt.expiration-ms=86400000

gemini.api.key=YOUR_GEMINI_API_KEY
gemini.api.model=gemini-3.6-flash

server.port=8080
```

Get a free Gemini API key at [aistudio.google.com](https://aistudio.google.com) — no credit card required.

```bash
cd backend
./mvnw spring-boot:run   # Mac/Linux
.\mvnw.cmd spring-boot:run   # Windows
```
Runs on `http://localhost:8080`.

### 3. Frontend
Create `frontend/.env`:
```
VITE_API_BASE_URL=http://localhost:8080
```

```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`.

### 4. First admin account
Admins aren't self-registered. Register a normal account via the UI, then promote it manually:
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your-admin-email@example.com';
DELETE FROM students WHERE user_id = (SELECT id FROM users WHERE email = 'your-admin-email@example.com');
```

---

## API Overview

| Area | Base path | Auth |
|---|---|---|
| Auth | `/api/auth/register`, `/api/auth/login` | Public |
| Student | `/api/students/**` | `ROLE_STUDENT` |
| Recruiter | `/api/recruiter/**` | `ROLE_RECRUITER` |
| Admin | `/api/admin/**` | `ROLE_ADMIN` |
| Chat | `/api/chat` | Any authenticated user |

All protected endpoints expect `Authorization: Bearer <token>`.

---

## Known Limitations

- **Branch matching for Recommended Jobs** is literal substring matching between the student's `branch` field and a job's `eligibleBranches` field, both free text. This means wording differences (e.g. "Computer Applications" vs "MCA") won't match even when they mean the same thing. A future version would replace free text with a shared enum/dropdown for both student profiles and job postings.
- **Admin accounts** are provisioned manually via direct database update rather than an admin-invite flow.
- **No password reset flow** yet.
- The recommendation logic and analytics are intentionally simple (filter/count-based) rather than a scoring or ML-based system — a deliberate v1 scope decision.

---

## What I Learned

This project was built end-to-end solo, covering:
- Designing a relational schema with proper foreign-key relationships across 7 tables
- Implementing stateless JWT authentication with Spring Security, including role-based method security
- Building a layered backend architecture (controller → service → repository → entity)
- A React frontend with protected, role-based routing and a consistent design system built from scratch
- Debugging real production-style issues: dependency version conflicts, stale build artifacts, deprecated third-party APIs, and Jackson/RestTemplate serialization conflicts
- Integrating a third-party AI API (Google Gemini) server-side to keep API keys secure

---

## License

This project was built for educational/portfolio purposes.
