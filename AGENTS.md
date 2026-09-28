# AGENTS.md — Military Asset Management System

## Project Goal
Build a full-stack Military Asset Management System for commanders and logistics personnel to track movement, assignment, and expenditure of assets (vehicles, weapons, ammunition) across multiple bases.

## Tech Stack (do not deviate)
- Backend: Java Spring Boot (Spring Web, Spring Security, Spring Data JPA)
- Frontend: React (shadcn/ui or MUI), Axios
- Database: PostgreSQL (relational — justify choice in README)
- Auth: JWT via Spring Security

## Repo Structure
- /backend  → Spring Boot app (Maven or Gradle)
- /frontend → React app
- /docs     → README, ER diagram notes, API docs

## Core Entities (JPA)
- User(id, name, username, password, role[ADMIN|BASE_COMMANDER|LOGISTICS_OFFICER], assignedBase)
- Base(id, name, location)
- Equipment(id, type, name, base, quantity, status)
- Purchase(id, base, equipmentType, quantity, date, createdBy)
- Transfer(id, fromBase, toBase, equipmentType, quantity, date, status, createdBy)
- Assignment(id, base, equipmentType, quantity, assignedToPersonnel, date, expended)
- AuditLog(id, user, action, endpoint, payload, timestamp)

## Features (build in this order)
1. Backend scaffold: entities, repositories, Flyway migrations, JWT auth, RBAC middleware (@PreAuthorize + custom filter reading role + base_id from JWT claims)
2. Purchases module: full CRUD API + React page (form + filterable history table)
3. Transfers module: API + React page (initiate transfer, history log with timestamps/status)
4. Assignments & Expenditures module: API + React page (assign to personnel, mark expended, history log)
5. Dashboard: aggregation API (opening balance, closing balance, net movement = purchases + transfers in − transfers out, assigned, expended) + React page with Date/Base/Equipment filters + modal breakdown on "Net Movement" click (bonus)
6. Global exception handling (@ControllerAdvice), API request/audit logging interceptor, input validation (@Valid + DTOs, frontend form validation)

## RBAC Rules
- ADMIN: full access, all bases
- BASE_COMMANDER: full access, own base only
- LOGISTICS_OFFICER: create/view purchases & transfers only, no assignments, no admin routes

## Non-Negotiable Requirements
- Every endpoint must be behind JWT auth + role check
- Every write action (purchase/transfer/assignment) must write to AuditLog
- Responsive React UI, no layout breakage on mobile widths
- Config values (DB creds, JWT secret) externalized in application.yml, never hardcoded

## Explicitly Out of Scope (for now)
- Deployment / hosting / CI-CD setup — do not touch Dockerfiles, cloud configs, or pipelines yet

## Definition of Done (per module)
- API endpoints tested manually or via a quick script/Postman collection
- React page wired to real API (no mock data left in place)
- Seed data present (via Flyway or CommandLineRunner) so the module is demoable immediately
- Short note added to /docs/README.md explaining what was built and any decisions made

## Working Style
- Work module by module per the order above — do not jump ahead
- After finishing a module, summarize what changed and what's next before continuing
- Ask before making any architectural decision not specified here (e.g., switching libraries)
