# NFC Employee Attendance Management System

Production-oriented employee attendance system with:

- **Backend:** Java 21, Spring Boot, Spring Security (JWT), Spring Data JPA, PostgreSQL, Flyway, OpenAPI
- **Admin frontend:** React + TypeScript + Vite (Hebrew RTL)
- **Employee app:** Native Android (Java) with NFC Reader Mode for NTAG213/NTAG215

```text
workspace/
├── backend/           # Spring Boot REST API
├── admin-frontend/    # React admin panel
├── android-app/       # Android employee NFC app
├── docker-compose.yml
└── .env.example
```

## Security model (important)

Standard NFC tags can be copied. Tag contents are **identifiers only**, never secrets and never proof of employee identity.

Implemented controls:

- Authenticated JWT on every attendance request
- Employee identity derived from the token (not client-supplied employee ID)
- Active station validation
- Duplicate / open-session prevention
- Short duplicate request window (rate limiting)
- Audit logs for clock events and admin corrections
- Role checks: `ADMIN` routes/APIs blocked for ordinary employees

Stronger anti-cloning options (documented, not fully implemented here):

- Cryptographic tags (e.g. NTAG 424 DNA) with challenge-response
- Rotating one-time payloads
- Device attestation / app integrity
- Geo-fencing / workplace Wi-Fi binding
- Tamper-evident physical tag mounts

## Quick start (Docker Compose)

```bash
cp .env.example .env
docker compose up --build
```

Services:

| Service | URL |
|---------|-----|
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Admin panel | http://localhost:5173 |

## Local development without full Compose

### 1. PostgreSQL

```bash
# using local Postgres or only the db service:
docker compose up -d db
```

Create DB/user matching `.env.example` (`nfc_attendance` / `nfc_dev_password`).

### 2. Backend

```bash
cd backend
mvn spring-boot:run
```

### 3. Admin frontend

```bash
cd admin-frontend
cp .env.example .env
npm install
npm run dev
```

Open http://localhost:5173

### 4. Android app

See [`android-app/README.md`](android-app/README.md) for APK build/install steps.

## First login

The admin login form is empty. On a new database, open the admin site and create the first admin account. Then add employees from **עובדים**. Optional demo NFC station `GATE-01` is created only when `APP_SEED_ENABLED=true`.

## Core API examples

### Login

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"YOUR_ADMIN","password":"YOUR_PASSWORD"}'
```

### Admin list employees

```bash
TOKEN=... # accessToken from login
curl -s http://localhost:8080/api/admin/employees \
  -H "Authorization: Bearer $TOKEN"
```

### Employee clock-in (identity from JWT)

```bash
EMP_TOKEN=...
curl -s -X POST http://localhost:8080/api/attendance/clock-in \
  -H "Authorization: Bearer $EMP_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"stationCode":"GATE-01","clientRequestId":"demo-1"}'
```

### Clock-out

```bash
curl -s -X POST http://localhost:8080/api/attendance/clock-out \
  -H "Authorization: Bearer $EMP_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"stationCode":"GATE-01","clientRequestId":"demo-2"}'
```

OpenAPI/Swagger: http://localhost:8080/swagger-ui.html

## Admin panel pages (Hebrew RTL)

1. Login
2. Dashboard
3. Employee management
4. Add/edit employee
5. Attendance records (+ correction with mandatory reason)
6. NFC stations
7. Work schedules
8. Reports / CSV & Excel export
9. Admin users
10. Audit log

Ordinary employees cannot access admin UI or `/api/admin/**`.

## Attendance rules

- Server UTC timestamps are authoritative; phone clock is ignored
- Prevents double clock-in while a session is open
- Prevents clock-out without an open session
- Worked minutes calculated on the backend (overnight supported via schedules)
- Grace / overtime / duplicate window configurable per workplace (`attendance_rules`)
- Admin corrections require a reason and keep original timestamp + audit history

## Tests

```bash
cd backend && mvn test
cd ../admin-frontend && npm test
```

## Production notes

- Set a long random `APP_JWT_SECRET`
- Use HTTPS termination (reverse proxy)
- Do not ship DB credentials or JWT secrets in frontend/mobile code
- Restrict CORS origins
- Replace cleartext Android network config with HTTPS only
