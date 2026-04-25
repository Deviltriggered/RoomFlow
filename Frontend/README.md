# RoomFlow Frontend

Angular client for RoomFlow.

## Environment

API URL is configured through Angular environment files:

- `src/environments/environment.ts` for development
- `src/environments/environment.prod.ts` for production

Production uses relative `/api` by default, so the same build can be served by the Spring Boot backend or a reverse proxy.

## Commands

```powershell
npm ci
npm start
npm run build
npm test
```
