# Military Asset Management System

A full-stack application for tracking military equipment inventory, purchases, transfers, assignments, and expenditures across bases. The project uses a Spring Boot REST API, a React web interface, and PostgreSQL.

## Features

- JWT login with role-based access for `ADMIN`, `BASE_COMMANDER`, and `LOGISTICS_OFFICER`.
- Base-scoped access for non-admin users.
- Purchase, transfer, assignment, and expenditure workflows with audit records for write actions.
- Dashboard balances and movement summaries with date, base, and equipment filters.
- Flyway-managed database schema and demo base/equipment data.

## Project layout

| Directory | Contents |
| --- | --- |
| `backend/` | Java 17 Spring Boot API, Spring Security, JPA, and Flyway migrations |
| `frontend/` | React + Vite interface using Material UI and Axios |
| `docs/` | Module details, API behavior, configuration, and implementation decisions |

## Requirements

- Java 17+
- Maven
- Node.js and npm
- PostgreSQL

## Configuration

Set these environment variables for the backend before starting it:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | PostgreSQL JDBC URL, for example `jdbc:postgresql://localhost:5432/military_asset_db` |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret, at least 32 UTF-8 bytes |
| `JWT_EXPIRATION_MS` | Optional token lifetime in milliseconds |
| `SERVER_PORT` | Optional backend port (default configured by the app) |
| `CORS_ALLOWED_ORIGINS` | Optional comma-separated allowed frontend origins |
| `BOOTSTRAP_ADMIN_USERNAME` | Optional username for initial admin creation |
| `BOOTSTRAP_ADMIN_PASSWORD` | Optional password for initial admin creation; set together with the username |

Database credentials and the JWT secret do not have hardcoded defaults. Flyway applies migrations on backend startup. PostgreSQL fits this system because bases, equipment, users, and movement records have relational constraints and inventory changes need transactional consistency.

## Run locally

1. Create a PostgreSQL database and set the required backend environment variables above.
2. Start the backend from `backend/`:

   ```bash
   mvn spring-boot:run
   ```

3. Install frontend dependencies and start Vite from `frontend/`:

   ```bash
   npm install
   npm run dev
   ```

Vite proxies `/api` requests to the local backend. For a deployed frontend build, set `VITE_API_BASE_URL` to the backend origin followed by `/api`.

## Authentication

`POST /api/auth/login` accepts a JSON username and password and returns a JWT bearer token. All application APIs require that token and enforce role/base access. `GET /api/auth/me` returns the authenticated user's profile. An initial admin can be created with the paired bootstrap admin variables.

## Documentation

See [docs/README.md](docs/README.md) for module-specific endpoints, authorization rules, dashboard calculations, and setup details.
