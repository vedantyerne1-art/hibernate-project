# TrustID Netlify Deployment Guide

## Important
Netlify can host the frontend only.
The Java Spring Boot backend must be deployed to a backend host (Render, Railway, Fly.io, Azure App Service, EC2, etc.).

## 1) Deploy backend first
Use the production steps in `docs/PRODUCTION_QUICKSTART.md` and expose backend over HTTPS.
Example backend URL:
- `https://api.yourdomain.com/api`

## 2) Configure backend CORS
Set backend env:
- `APP_ALLOWED_ORIGINS=https://your-netlify-site.netlify.apps`

If you use a custom frontend domain, add that too:
- `APP_ALLOWED_ORIGINS=https://app.yourdomain.com`

## 3) Deploy frontend to Netlify
This repo includes `frontend/netlify.toml`.

In Netlify:
- Connect this repo
- Build command: `npm ci && npm run build`
- Publish directory: `dist`
- Base directory: `frontend`

## 4) Set frontend environment variable in Netlify
Add env var in Netlify site settings:
- `VITE_API_BASE_URL=https://api.yourdomain.com/api`

Then trigger a redeploy.

## 5) Verify production
Open frontend URL and test:
- Register/login
- Upload profile photo
- Public QR verify page

## 6) Common failure cases
- **Frontend loads but API fails**: `VITE_API_BASE_URL` missing/wrong.
- **CORS blocked**: `APP_ALLOWED_ORIGINS` does not include frontend domain.
- **Photo not visible in QR page**: backend not restarted after latest code, or upload file path inaccessible on backend host.

## 7) Optional domains
- Netlify frontend: `app.yourdomain.com`
- Backend host: `api.yourdomain.com`

This keeps frontend static hosting and backend API cleanly separated.
