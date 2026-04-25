# RoomFlow

RoomFlow is a full-stack booking service for rentable rooms and workspaces. It includes a Spring Boot backend, an Angular frontend, JWT authentication, client bookings, payment summaries, and an admin area for managing bookings and locations.

This repository is prepared for a public `1.0.0` release: database credentials, JWT secrets, admin email, and deployment-specific origins are configured through environment variables instead of hardcoded project data.

## Features

- User registration and login with stateless JWT authentication.
- Access token and refresh token flow.
- Room and tariff catalog.
- Availability lookup by date and location.
- Booking creation for authenticated users.
- User booking and payment history.
- Admin booking status management.
- Admin location create/update/delete workflow.
- Angular production build served as static resources by the backend.

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Security
- Spring Data JPA
- PostgreSQL
- Angular 17
- RxJS
- Maven multi-module build

## Project Structure

```text
.
├── Backend/        Spring Boot API and static frontend host
├── Frontend/       Angular application
├── mvnw.cmd        Maven wrapper for Windows
├── run-roomflow.*  Local launch helpers
└── .env.example    Example environment configuration
```

## Configuration

Use `.env.example` as a template, then set the variables in your shell or deployment platform. Do not commit real secrets.

Required variables:

```env
DB_URL=jdbc:postgresql://localhost:5432/roomflow
DB_USERNAME=roomflow_user
DB_PASSWORD=change_me_before_running
JWT_SECRET=change-me-to-a-random-secret-with-at-least-32-characters
```

Optional variables:

```env
APP_PORT=8080
JWT_ACCESS_TOKEN_TTL=30m
JWT_REFRESH_TOKEN_TTL=14d
APP_ADMIN_EMAIL=admin@example.com
APP_CORS_ALLOWED_ORIGINS=http://localhost:4200,http://127.0.0.1:4200
```

`APP_ADMIN_EMAIL` promotes the matching registered user to `ADMIN`. Leave it empty if you do not want automatic admin promotion.

PowerShell example:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/roomflow"
$env:DB_USERNAME = "roomflow_user"
$env:DB_PASSWORD = "change_me_before_running"
$env:JWT_SECRET = "change-me-to-a-random-secret-with-at-least-32-characters"
```

## Frontend Environments

The Angular app uses environment files:

- Development: `Frontend/src/environments/environment.ts`
- Production: `Frontend/src/environments/environment.prod.ts`

Development points to `http://localhost:8080/api`. Production uses `/api`, which is suitable when frontend and backend are served from the same origin or behind a reverse proxy.

## Local Development

Install frontend dependencies:

```powershell
cd Frontend
npm ci
```

Run the Angular dev server:

```powershell
npm start
```

Run backend tests:

```powershell
.\mvnw.cmd -f Backend\pom.xml test
```

Build the full project:

```powershell
.\mvnw.cmd clean package
```

Run the packaged backend after setting environment variables:

```powershell
.\run-roomflow.ps1 -Rebuild
```

Or with `cmd`:

```cmd
run-roomflow.cmd --rebuild
```

The backend starts on `APP_PORT` or `8080`.

## Authentication Flow

1. `POST /api/auth/login` or `POST /api/auth/register` returns `user`, `accessToken`, and `refreshToken`.
2. The frontend stores tokens in `localStorage`.
3. Protected requests include `Authorization: Bearer <accessToken>`.
4. The backend validates the JWT signature and expiration.
5. The backend reconstructs `AuthenticatedUser` from token claims and places it into Spring Security's `SecurityContext`.
6. The frontend refreshes the access token shortly before expiration through `POST /api/auth/refresh`.

Current logout is client-side only: tokens are removed from `localStorage`, but already issued tokens remain valid until expiration.

## Production Notes

- Set a strong `JWT_SECRET` with at least 32 characters.
- Use a dedicated PostgreSQL user with limited privileges.
- Keep `.env` files out of Git.
- Set `APP_CORS_ALLOWED_ORIGINS` to exact frontend origins.
- Prefer serving the frontend and backend behind one domain and proxying `/api` to the backend.
- Consider refresh token rotation and server-side token revocation for stronger logout semantics.

## Version

Current release target: `1.0.0`.
