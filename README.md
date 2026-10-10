# Keepwell | Personal Document Expiry Tracker

Keepwell tracks private document details, expiry dates, renewal history, and in-app reminders for each account. The existing Spring Boot application remains the backend and server-rendered interface; a standalone HTML/CSS/JavaScript frontend in `frontend/` can also use its JSON API.

## Existing features

- Email/password account registration and sign-in with BCrypt password hashes.
- Private document metadata: name, category, optional reference number, issue and expiry dates, and notes.
- Expiry status bands: expired, due within 7 days, due within 30 days, and current.
- Renewal history with previous/new expiry dates, renewal time, and optional notes.
- Daily in-app reminders from 10 days before expiry through the expiry date; users can mark reminders read.
- Document and reminder operations are scoped to the authenticated account. State-changing web and API requests require CSRF protection.

Keepwell stores document metadata only. It does not upload document files, send email/push notifications, or currently support deleting documents.

## Architecture

- **Backend:** Java 17, Spring Boot 3.5, Spring MVC, Spring Security, Spring Data JPA/Hibernate, Bean Validation, and Flyway.
- **Frontend:** standalone Vite-built HTML, CSS, and JavaScript; the design reuses the original `app.css`.
- **Database:** MySQL 8.4. H2 is used by automated tests only.
- **Authentication:** Spring Security sessions and CSRF. The Vercel frontend sends credentialed requests and an `X-CSRF-TOKEN` header for writes. There are no browser-stored bearer tokens.

The original Thymeleaf pages and routes are retained; the independent Vercel frontend uses `/api/**` JSON endpoints.

## JSON API

JSON API routes are rooted at `/api`. The CSRF endpoint returns a token for the current session. Authenticated API requests require the session cookie; POST/PUT requests require the CSRF token.

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/health` | Public | Render health check |
| `GET` | `/api/auth/csrf` | Public | Start/retrieve a session and obtain its CSRF token |
| `POST` | `/api/auth/register` | Public + CSRF | Register with `{ "name", "email", "password" }` |
| `POST` | `/api/auth/login` | Public + CSRF | Sign in with `{ "email", "password" }` |
| `GET` | `/api/auth/me` | Authenticated | Current account's safe profile |
| `POST` | `/api/auth/logout` | Authenticated + CSRF | End the session |
| `GET` | `/api/dashboard` | Authenticated | Documents, status counts, recent reminders, unread count |
| `GET` | `/api/documents/categories` | Authenticated | Supported categories and labels |
| `GET` | `/api/documents` | Authenticated | Current account's documents |
| `POST` | `/api/documents` | Authenticated + CSRF | Create a document (returns `201 Created`) |
| `GET` | `/api/documents/{id}` | Owner only | Document detail and renewal history |
| `PUT` | `/api/documents/{id}` | Owner only + CSRF | Update document metadata |
| `POST` | `/api/documents/{id}/renew` | Owner only + CSRF | Record a renewal |
| `GET` | `/api/documents/{id}/renewals` | Owner only | Renewal history |
| `POST` | `/api/reminders/{id}/read` | Owner only + CSRF | Mark a reminder read |

Document create/update JSON uses `title`, `category` (the enum value, for example `IDENTIFICATION`), `referenceNumber`, `issueDate`, `expiryDate`, and `notes`. Dates are ISO `YYYY-MM-DD`. API errors include an `error` message and, when applicable, a `fields` map. A document owned by another account is returned as not found. There is deliberately no delete endpoint because deletion was not supported by the existing application.

## Run locally

Requirements: Java 17+, Maven 3.6+, Node.js 20+, and Docker Desktop or a local MySQL 8.4 instance.

1. Start the development database:

   ```powershell
   docker compose up -d
   ```

   Alternatively, point `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` at a local MySQL 8.4 database.

2. In the project root, run the backend:

   ```powershell
   mvn spring-boot:run
   ```

3. In another terminal, configure and start the standalone frontend:

   ```powershell
   cd frontend
   Copy-Item .env.example .env.local
   npm install
   npm run dev
   ```

   The Vite development URL is usually `http://localhost:5173`; the default API URL is `http://localhost:8080`. Open the Vite URL to use the standalone frontend, or open `http://localhost:8080` for the original Thymeleaf application.

The local Compose credentials are for development only. `docker compose down` stops MySQL but preserves its `tracker_mysql` volume. To remove the volume as well, explicitly run `docker compose down -v`.

## Configuration

| Variable | Default | Use |
| --- | --- | --- |
| `PORT` | `8080` | HTTP port; Render supplies this at runtime |
| `DB_URL` | local MySQL URL in `application.properties` | JDBC URL of the persistent MySQL 8.4 database |
| `DB_USERNAME` | `tracker` | Database username |
| `DB_PASSWORD` | `tracker_local` | Database password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Exact comma-separated frontend origins; do not use `*` |
| `JPA_DDL_AUTO` | `update` | Local schema behavior; use `validate` in production |
| `FLYWAY_ENABLED` | `true` | Apply versioned production schema migrations |
| `FLYWAY_BASELINE_ON_MIGRATE` | `true` | Baseline an existing non-empty schema before applying future migrations |
| `SESSION_COOKIE_SAME_SITE` | `lax` | Set to `none` for cross-site Vercel/Render cookies |
| `SESSION_COOKIE_SECURE` | `false` | Set to `true` when using HTTPS |
| `REMINDER_CRON` | `0 0 8 * * *` | Daily reminder schedule in server time |

The initial production schema is in `src/main/resources/db/migration`. Flyway applies it on a new database; Hibernate validates the schema on Render. Baseline-on-migrate also lets existing local installations (whose tables predate Flyway) continue to start; on Render, review and back up an existing schema before baselining it. Render's MySQL-compatible connection must be reachable from the service and should use TLS where the database provider supports it. Production database credentials belong in the Render dashboard, never in source control or frontend variables.

## Deploy the backend to Render

1. Create a persistent managed MySQL 8.4 database with a MySQL provider that is reachable from Render. Render web services do not provide the MySQL server for this application; configure database backups/retention with the provider. Do not use H2 in production.
2. Push this project to your Git repository and create a Render Blueprint using the root `render.yaml`, or create a Docker web service with the root `Dockerfile`.
3. Set the backend environment variables in Render:
   - `DB_URL`: the provider's JDBC URL, beginning `jdbc:mysql://`, including the database name and provider-required TLS options.
   - `DB_USERNAME` and `DB_PASSWORD`: database credentials, entered as secret environment values.
   - `CORS_ALLOWED_ORIGINS`: the exact HTTPS Vercel domain, without a trailing slash; comma-separate additional exact preview domains only if needed.
   - Keep `JPA_DDL_AUTO=validate`, `FLYWAY_ENABLED=true`, `FLYWAY_BASELINE_ON_MIGRATE=true`, `SESSION_COOKIE_SAME_SITE=none`, and `SESSION_COOKIE_SECURE=true`.
4. Wait for the Render health check at `/api/health`. The service listens on Render's assigned `PORT`.

When connecting an existing non-empty database, review its schema and take a backup before enabling Flyway baseline-on-migrate. Flyway records the baseline without rewriting existing tables; Hibernate validation will fail startup if the existing schema does not match the application entities.

## Deploy the frontend to Vercel

1. Import the repository into Vercel and set **Root Directory** to `frontend`.
2. Set **Build Command** to `npm run build` and **Output Directory** to `dist`. The Vite configuration builds the independent HTML pages; no SPA rewrite is required.
3. Add `VITE_API_BASE_URL` to Vercel's Production (and Preview, if used) environment variables. Set it to the Render service's HTTPS origin, such as `https://<your-render-service>.onrender.com`—without inventing a URL or including a `/api` suffix.
4. After Vercel assigns the frontend domain, set that exact HTTPS origin as Render's `CORS_ALLOWED_ORIGINS`, then redeploy the backend if necessary.

`VITE_API_BASE_URL` is public frontend configuration, not a secret. Never put database credentials in Vercel. The frontend uses `credentials: "include"`; therefore production sessions need `SameSite=None; Secure` and the backend must use HTTPS.

**Cookie limitation:** Vercel and Render default domains are cross-site. Some browsers block third-party cookies, which can prevent session sign-in even when CORS and cookie attributes are correct. For dependable production sign-in, use custom domains under the same registrable domain (for example, separate `app` and `api` subdomains) or configure a same-site Vercel proxy. Restrict CORS to the final browser-visible frontend origin; do not loosen CORS or disable CSRF to work around cookie restrictions.

## Tests

Run the backend tests:

```powershell
mvn test
```

Run the frontend production build:

```powershell
cd frontend
npm ci
npm run build
```

Backend API tests use H2 only as an isolated test database and cover registration, BCrypt-safe responses, login/logout, CSRF, CORS, authentication, account ownership, validation, documents, renewals, and reminders.
