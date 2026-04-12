# ExamPortal — LAN-Based Online Examination System

A secure, LAN-based online examination platform with real-time proctoring, blueprint-driven exam generation, and scalable multi-user support.

Designed for institutions to conduct exams over a local network (LAN) without requiring internet connectivity.

---
## Key Highlights
- Secure JWT-based authentication system
- Blueprint-driven exam generation engine
- Real-time evaluation with subject-wise analytics
- Bulk question upload via Excel (Apache POI)
- Timed exams with auto-save and resume support
- Advanced proctoring with violation detection & auto-submit
- LAN-based deployment (no internet required)

---

## Screenshots

### Admin dashboard
![Admin dashboard](screenshots/admin_dash.png)
*Platform overview showing total students, teachers, and subjects at a glance.*

### Blueprints & exam setup
![Blueprint creation](screenshots/admin_blueprint.png)
*Create blueprints with per-subject question counts, marks, and optional section labels. Exams are built from blueprints.*

### Live exam interface
![Exam in progress](screenshots/exam.png)
*GATE/JEE-style exam UI: countdown timer, question navigator with colour-coded status, mark-for-review, and submit.*

### Exam result
![Result page](screenshots/result.png)
*Score card with correct/wrong/unattempted breakdown, subject-wise analysis table, and one-click PDF download.*

### Teacher — question bank
![Question upload](screenshots/question_upload.png)
*Teachers upload questions one at a time or in bulk via Excel. Live question bank shown on the right.*

---

## Features

**Admin**
- Platform overview (student, teacher, and subject counts)
- Approve pending teacher accounts
- Create and manage subjects
- Build exam blueprints (subject → question count → marks/negatives)
- Create and publish exams with scheduled start/end windows
- View all student results with subject-wise breakdown and violation logs

**Teacher**
- Upload questions manually or via `.xlsx` bulk upload (Apache POI)
- Browse and manage per-subject question bank
- View exam results for all assigned exams

**Student**
- Dashboard showing live and upcoming exams
- Fullscreen-enforced exam session with per-student question shuffle
- Colour-coded question navigator (Not Visited / Not Answered / Answered / Marked for Review / Answered & Marked)
- Auto-save every 30 seconds; resumes if session is interrupted
- Timer with warning colours; auto-submit on expiry
- Result page with score, breakdown, and PDF download

**Proctoring**
- Fullscreen enforcement with grace-period modal on exit
- Tab-switch detection
- Copy/paste and right-click blocking
- Keyboard shortcut blocking (F12, Ctrl+Shift+I, etc.)
- DevTools size heuristic
- Mouse-leave debounced detection
- Configurable violation threshold → auto-submit

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, React Router 6, Axios |
| Backend | Spring Boot 3.2, Spring Security, JWT (HS256) |
| Database | PostgreSQL |
| Excel parsing | Apache POI 5.2.5 |
| PDF generation | iText 7 |
| Auth | Stateless JWT, BCrypt password hashing |
| Deployment | Single JAR + `application.yml` |

---

## Architecture

```
Student PCs / Teacher PC
        │  HTTP over LAN (192.168.x.x:8080)
        ▼
┌─────────────────────────────────────────┐
│         Spring Boot JAR                 │
│  ┌──────────────┐  ┌─────────────────┐  │
│  │ React build  │  │  REST API       │  │
│  │ /static      │  │  /api/**        │  │
│  └──────────────┘  └────────┬────────┘  │
│                             │           │
│  ┌──────────────────────────▼────────┐  │
│  │  JWT filter → Service layer       │  │
│  │  ExamService · AttemptService     │  │
│  │  EvaluationService · ProctorSvc   │  │
│  └──────────────────────────┬────────┘  │
│                             │           │
│  ┌──────────────────────────▼────────┐  │
│  │  JPA Repositories → PostgreSQL    │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

The React build is served directly from Spring Boot's static resources — no separate Node server needed.

---

## Project Structure

```
backend_final/
├── src/main/java/com/examportal/
│   ├── auth/          # JWT service, filter, login/register
│   ├── user/          # User entity, roles (ADMIN, TEACHER, STUDENT)
│   ├── subject/       # Subject CRUD
│   ├── question/      # Question bank, Excel upload
│   ├── exam/          # Blueprint, Exam, publish engine
│   ├── attempt/       # Exam session, autosave, submit
│   ├── evaluation/    # Marking, result storage
│   ├── proctor/       # Violation tracking, auto-submit
│   ├── reporting/     # iText7 PDF generation
│   ├── admin/         # Stats, teacher approval
│   └── common/        # ApiResponse, GlobalExceptionHandler
└── src/main/resources/
    ├── application.yml
    └── db/schema.sql

frontend_final/
└── src/
    ├── api/           # axiosConfig, authApi, examApi, attemptApi, adminApi
    ├── context/       # AuthContext (JWT storage)
    ├── hooks/         # useTimer, useViolationDetector
    ├── pages/         # LoginPage, StudentDashboard, ExamPage, ResultPage,
    │                  # TeacherDashboard, AdminDashboard
    └── components/    # ExamResultsViewer, Spinner, Badge
```

---

## Prerequisites

- Java 17+
- Node.js 18+ and npm
- PostgreSQL 14+
- Maven 3.8+

---

## Setup & Running

### 1. Database

Create the database and run the migration script:

```sql
CREATE DATABASE "ExamSystemDB";
\c "ExamSystemDB"
\i backend_final/src/main/resources/db/schema.sql
```

### 2. Configure Environment Variables

Create .env file in the project root folder :

```
DB_URL=jdbc:postgresql://localhost:5432/ExamSystemDB
DB_USERNAME=your_username
DB_PASSWORD=your_password
JWT_SECRET=your_secret_key
MAIL_USERNAME=your_email
MAIL_PASSWORD=your_app_password
```

### 3. Build and run the backend

```bash
cd backend_final
mvn clean install
mvn spring-boot:run
```

The backend starts on `http://0.0.0.0:8080/api`.

### 4. Build the frontend

```bash
cd frontend_final
npm install
npm run build
```

Copy the `build/` output into `backend_final/src/main/resources/static/` so Spring Boot serves it, then rebuild the JAR.

### 5. LAN access

Find the server machine's LAN IP (e.g. `192.168.1.10`). Students and teachers open:

```
http://192.168.1.10:8080
```

No additional configuration is needed on client machines.

---

## Excel Question Upload

Teachers can upload questions in bulk using an `.xlsx` file.

| Column | Field | Required |
|---|---|---|
| A | `subject_id` | ✅ Numeric DB id |
| B | `question_text` | ✅ |
| C | `option_a` | ✅ |
| D | `option_b` | ✅ |
| E | `option_c` | ✅ |
| F | `option_d` | ✅ |
| G | `correct_option` | ✅ `0`=A `1`=B `2`=C `3`=D |
| H | `difficulty` | optional (`EASY`/`MEDIUM`/`HARD`) |
| I | `marks` | optional (default `1`) |
| J | `negative_marks` | optional (default `0.25`) |

Row 1 is the header and is skipped. Rows with an empty column A are also skipped.

---

## API Overview

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/auth/register` | public | Register new user |
| POST | `/auth/login` | public | Login → JWT |
| GET | `/exams/active` | STUDENT | Live exams |
| POST | `/attempts/start/{examId}` | STUDENT | Start exam session |
| PUT | `/attempts/{id}/answers` | STUDENT | Autosave answers |
| POST | `/attempts/{id}/submit` | STUDENT | Submit exam |
| GET | `/results/{attemptId}` | STUDENT | View own result |
| GET | `/results/pdf/{attemptId}` | STUDENT | Download result PDF |
| POST | `/questions` | TEACHER | Upload single question |
| POST | `/questions/excel` | TEACHER | Bulk upload via xlsx |
| POST | `/blueprints` | ADMIN | Create blueprint |
| POST | `/exams` | ADMIN | Create exam |
| POST | `/exams/{id}/publish` | ADMIN | Publish exam |
| GET | `/results/exam/{id}/students` | ADMIN/TEACHER | All student results |
| PUT | `/admin/teachers/{id}/approve` | ADMIN | Approve teacher |

All protected endpoints require `Authorization: Bearer <token>` header.

---

## Proctoring Configuration

Adjust thresholds in `application.yml`:

```yaml
exam:
  proctor:
    max-violations: 1          # hard violations before auto-submit
    max-fullscreen-exits: 1    # fullscreen exits before auto-submit
    fullscreen-grace-seconds: 10  # countdown shown in the warning modal
```

---

## Default Accounts

Register accounts through the UI. The first ADMIN account must be seeded directly in the database or via postman (recommended):

```sql
INSERT INTO users (full_name, email, password, role, approved)
VALUES ('System Admin', 'admin@exam.local',
        '$2a$10$...bcrypt_hash...', 'ADMIN', true);
```

Teachers register via the UI and must be approved by an Admin before they can log in.

---

## License

MIT — see [LICENSE](./LICENSE) for details.
