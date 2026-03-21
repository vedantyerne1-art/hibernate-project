# Gmail OTP Setup (TrustID)

This project already supports Gmail SMTP in `MailConfig`.
You only need to provide environment variables.

## 1) Create Google App Password
1. Open Google Account settings.
2. Enable 2-Step Verification.
3. Go to Security -> App passwords.
4. Create an app password (Mail).
5. Copy the 16-character password.

## 2) Configure local env
1. Copy `backend/.env.local.example` to `backend/.env.local`.
2. Set:
   - `GMAIL_USERNAME=your_email@gmail.com`
   - `GMAIL_APP_PASSWORD=your_16_char_app_password`

## 3) Start app
Run:
```bat
start_all.bat
```

The script auto-loads `backend/.env.local` and backend OTP emails will be sent through Gmail.

## 4) Verify OTP send
Call API:
```http
POST /api/auth/send-verification
Content-Type: application/json

{"email":"your_email@gmail.com"}
```

If configured correctly, OTP email will arrive in Gmail inbox.

## Notes
- Do not commit `.env.local`.
- Gmail is required now. Backend startup will fail if `GMAIL_USERNAME` or `GMAIL_APP_PASSWORD` is missing.
