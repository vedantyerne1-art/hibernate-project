@echo off
echo Starting TrustID System Integration
echo.

if exist backend\.env.local (
	echo Loading environment from backend\.env.local
	for /f "usebackq tokens=1,* delims==" %%A in ("backend\.env.local") do (
		if not "%%A"=="" (
			if /I not "%%A"=="REM" (
				if not "%%A:~0,1"=="#" (
					set "%%A=%%B"
				)
			)
		)
	)
)

echo [1/3] Starting Infrastructure (Postgres)
docker-compose up -d db

echo.
echo [2/3] Building Backend (Requires Maven installed globally or locally)
cd backend
REM Fallback to local mvn if mvnw fails
call mvn clean install -DskipTests
start "TrustID Backend" cmd /k "mvn spring-boot:run"
cd ..

echo.
echo [3/3] Starting Frontend
cd frontend
start "TrustID Frontend" cmd /k "npm install && npm run dev"
cd ..

echo.
echo TrustID Environment is launching...
echo - Backend: http://localhost:8080
echo - Frontend: http://localhost:5173
