# Military Asset Management System

## Module 1: backend scaffold

The backend is a Java 17 Spring Boot application under `backend/`, built with Maven. It defines the seven JPA entities, matching Spring Data repositories, a PostgreSQL schema managed by Flyway, stateless JWT login, and method-level role authorization. JWTs carry `user_id`, `role`, and `base_id`; the authentication filter validates the signature and expiration, then creates an authenticated principal. The `baseScope` bean supports per-base checks in `@PreAuthorize` expressions. `ADMIN` can pass any base check; other roles must match their assigned base.

PostgreSQL is used because the asset, base, movement, assignment, user, and audit records have strong relational links and benefit from foreign-key constraints and transactions to protect inventory data. Hibernate runs in `validate` mode so Flyway remains the schema source of truth. Migrations create all tables and seed two demo bases and inventory records. The optional `BootstrapAdmin` runner creates an initial admin from the configured bootstrap credentials when that username is absent.

## Module 2: purchases

Added authenticated purchase endpoints: `POST /api/purchases`, filtered `GET /api/purchases`, `GET /api/purchases/{id}`, `PUT /api/purchases/{id}`, and `DELETE /api/purchases/{id}`. List filters accept `baseId`, inclusive `fromDate`/`toDate` (`YYYY-MM-DD`), and case-insensitive exact `equipmentType`. `GET /api/bases` supplies the real base choices used by the frontend. Admins can select any base. Base commanders and logistics officers are restricted to their JWT `base_id`; logistics officers can create and view purchases, while update and delete are limited to admins and base commanders. Create, update, and delete transactions write AuditLog rows atomically with the purchase change.

The new React/Vite app in `frontend/` uses Axios and Material UI. It authenticates through the existing login API, retains the returned JWT in browser local storage, and loads bases and purchase history from the backend. Its responsive purchases screen supports create, filter, and role-appropriate edit/delete actions. On an empty purchase table, the backend creates two audited demo purchase entries after bootstrap-user creation.

## Module 3: transfers

Added authenticated `POST /api/transfers`, filtered `GET /api/transfers`, `GET /api/transfers/{id}`, `PATCH /api/transfers/{id}/status`, and `GET /api/transfers/bases`. A non-admin initiates transfers only from their assigned base and can view a transfer only when that base is its origin or destination. The base directory exposes only base names and locations for destination selection. New transfers begin as `PENDING`; admins and base commanders can move a transfer from `PENDING` to `IN_TRANSIT` or `REJECTED`, then from `IN_TRANSIT` to `COMPLETED` or `REJECTED`. Logistics officers can create and view but cannot change status. A Flyway migration adds a creation timestamp, and create/status changes write AuditLog entries in the same transaction.

The frontend now includes a Transfers tab with a real-API initiation form, filters, and a responsive history table showing route, equipment type, quantity, transfer date, recorded timestamp, creator, and status. Admins can filter across bases; non-admin transfer queries are always constrained to transfers touching their assigned base.

## Module 4: assignments and expenditures

Added authenticated `GET /api/assignments`, `GET /api/assignments/{id}`, `POST /api/assignments`, `POST /api/assignments/{id}/expend`, and supporting `GET /api/assignments/bases` and `GET /api/assignments/equipment` endpoints. Only admins and base commanders can use this module; admins can select any base, while base commanders are restricted to their assigned base. Logistics officers are denied by the controller role check and do not see the frontend tab. Assignment creation locks and checks the matching base equipment row, rejects unavailable/insufficient stock, and decrements available quantity in the same transaction as the assignment and audit record. Marking an assignment expended is a one-way action and is audited. A Flyway migration adds the assignment creation timestamp; an empty assignment table receives one audited demo assignment when stock and a user are available.

The frontend Assignments & Expenditures tab uses live endpoints for base/equipment choices, available quantities, assignment submission, filters, history, and marking assignments expended. History shows the assignment date, recorded timestamp, base, equipment, personnel, quantity, and assigned/expended state.

## Module 5: dashboard

Added `GET /api/dashboard` with inclusive `fromDate`/`toDate`, optional `baseId`, and case-insensitive exact `equipmentType` filters. The response includes opening and closing balances, net movement, purchases, completed transfers in/out, assigned quantity, and expended quantity. Opening balance is reconstructed from current available stock by reversing transactions dated on/after the period start: assignments and transfers out are added back, while purchases and transfers in are subtracted. Closing balance is opening balance plus period purchases and completed transfers in, minus completed transfers out and assignments. Expended quantity is a subset of assignments and is displayed separately. Only completed transfers count as movement; pending, in-transit, and rejected transfers do not.

Dashboard access is limited to admins and base commanders because the metrics include personnel assignments and expenditures. Admins can select all bases or aggregate all bases; base commanders are always scoped to their JWT base, including when a different `baseId` is requested. The React Dashboard tab uses the live endpoint, supports date/base/equipment filters, and opens a net-movement dialog with purchases, transfer in, transfer out, and net total. The existing navigation hides Dashboard from logistics officers.

Configuration is supplied through environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 UTF-8 bytes), `JWT_EXPIRATION_MS`, `PORT` (hosting platform) or `SERVER_PORT`, and `CORS_ALLOWED_ORIGINS` (comma-separated frontend origins). Optional `BOOTSTRAP_ADMIN_USERNAME` plus `BOOTSTRAP_ADMIN_PASSWORD` create an initial ADMIN with a BCrypt password hash when that username is absent. Set both or neither. Database credentials and the JWT secret have no hardcoded defaults.

### Run

Create a PostgreSQL database and set `DB_URL` to a JDBC URL such as `jdbc:postgresql://localhost:5432/military_asset_db?sslmode=require`, plus `DB_USERNAME`, `DB_PASSWORD`, and a `JWT_SECRET` of at least 32 bytes; optionally set the bootstrap admin username/password. Run `mvn spring-boot:run` from `backend/`, then run `npm install` and `npm run dev` from `frontend/`. Vite proxies `/api` requests locally; production builds can set `VITE_API_BASE_URL` to the deployed backend origin plus `/api`. Set backend `CORS_ALLOWED_ORIGINS` to the deployed frontend origin. Flyway applies migrations on startup. `POST /api/auth/login` accepts `{ "username": "...", "password": "..." }` and returns a bearer token. `GET /api/auth/me` is a protected role-checked endpoint. Login is the sole unauthenticated route so a user can obtain a token; all other routes require JWT authentication.

Per-base authorization helper example: `@PreAuthorize("hasRole('ADMIN') or @baseScope.allows(authentication, #baseId)")`. Module endpoints will add the role and base constraints appropriate to each operation as they are implemented.
