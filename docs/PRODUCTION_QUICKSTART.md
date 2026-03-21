# TrustID Production Quickstart

## 0) Automated preflight (recommended)
Windows PowerShell:
```powershell
./deployment/preflight-prod.ps1 -EnvFile backend/.env.prod
```

## 1) Prepare environment variables
Use `backend/.env.prod.example` as a template and set real values.

## 2) Database migration
Ensure target PostgreSQL database exists and Flyway can connect.

## 3) Build backend
```bash
cd backend
mvn clean package -DskipTests
```

## 4) Start backend in production mode
Windows CMD:
```bat
set SPRING_PROFILES_ACTIVE=prod
set DB_URL=jdbc:postgresql://<db-host>:5432/<db-name>
set DB_USERNAME=<db-user>
set DB_PASSWORD=<db-password>
set JWT_SECRET=<long-random-secret>
set APP_ALLOWED_ORIGINS=https://your-frontend-domain.com
mvn spring-boot:run
```

Windows PowerShell (automated package + run):
```powershell
./deployment/start-prod-backend.ps1 -EnvFile backend/.env.prod
```

## 5) Health check
```bash
curl http://localhost:8080/actuator/health
```

## 6) Security minimums
- Run only behind HTTPS reverse proxy.
- Restrict CORS to your frontend domain only.
- Rotate JWT secret and DB credentials periodically.
- Back up database and uploads regularly.

## 7) Optional OTP mail delivery
If `GMAIL_USERNAME` + `GMAIL_APP_PASSWORD` are set, OTP emails go to Gmail SMTP.
If not set, app falls back to local Mailpit (`localhost:1025`).
