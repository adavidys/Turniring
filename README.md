# Turniring

Backend platform for programming tournaments with roles, team registration, tasks, submissions, jury evaluation, announcements, schedule, profile data, and leaderboard export.

## Local infrastructure

The root `docker-compose.yml` runs a TLS-terminated stack for deployment:

- PostgreSQL (internal network only)
- Spring Boot backend (internal network only)
- Vue frontend (internal network only)
- Nginx gateway on ports `80` and `443`

### Secure Docker deploy with OpenSSL certificates

```bash
cp .env.example .env
vim .env
./scripts/generate-tls-certs.sh localhost
docker compose up --build -d
```

Open the app at `https://localhost` (self-signed certificate by default).

Required variables are in `.env.example`:

- `POSTGRES_PASSWORD`
- `APP_JWT_SECRET` (at least 32 chars)
- `APP_BOOTSTRAP_ADMIN_PASSWORD`

Only the TLS gateway is publicly exposed (`80` and `443`). PostgreSQL, backend, and frontend remain internal to the Docker network.

If you change `POSTGRES_PASSWORD` after first startup, recreate the PostgreSQL volume once:

```bash
docker compose down --volumes
```

### Security checks (all files + deploy checks)

```bash
./scripts/security-checks.sh
./scripts/deploy-smoke-test.sh
```

This runs:
- gitleaks over all repository files
- trivy filesystem scan (`vuln`, `misconfig`, `secret`)
- Docker Compose config validation

## Frontend

A Vue 3 SPA now lives in `frontend/`.

### Frontend stack

- Vue 3
- Vue Router
- Vite

### Run frontend locally

```bash
cd frontend
npm install
npm run dev
```

Vite proxies `/api` requests to `http://127.0.0.1:8080` by default, so the Spring backend should be running locally. In Docker Compose, Nginx terminates TLS and routes `/api` traffic to the backend service.

### Frontend pages

- Public home page with grouped tournaments
- Tournament details page with announcements, schedule, teams, tasks, leaderboard, and team registration form
- Login and registration
- Profile page
- Team workspace for roster editing and submissions
- Jury workspace for evaluations
- Admin dashboard for tournament operations
- Admin user creation page (TEAM/ORGANIZER/ADMIN)
- Olympiad creation page for admins/organizers
- Team creation page for registered users
- Jury creation page for admins/organizers
- Public olympiad join list (open registration)
- Public team list by olympiad
- One-button theme switch (dark / white)
- Home filters: Registration open / Running / Finished
- Team quick block on home: your tournament, active task, and latest submission

## Roles

- `ADMIN` or `ORGANIZER`: manage tournaments, tasks, announcements, schedule, jury assignments, CSV export
- Any authenticated role (`USER`, `TEAM`, `JURY`, `ADMIN`, `ORGANIZER`): create a team, join/leave olympiads during registration, manage roster, submit work
- `JURY`: view assigned submissions and submit evaluations

Self-registration (`/api/auth/registration`) allows choosing one role: `TEAM` or `ADMIN` (`USER` is still accepted for backward compatibility).

Registered users can also change their own role from profile via `PUT /api/profile/me/role` (`USER`, `ADMIN`). Team participation is managed through team pages, and role switching is blocked while the user belongs to a team.

Users with role `ADMIN` or `JURY` can update their own account data (name, last name, email, optional password) via `PUT /api/profile/me`.

Role `JURY` is granted only through invite links.

Admins/organizers can generate UUID invite links for jury or team membership. Invite links are active for 1 hour. After successful acceptance, the link is deleted. Opening an expired/used/non-existent invite returns: `This link is not active`.

## Default Admin

The application bootstraps a default admin on startup if it does not exist:

- email: `admin@turniring.local`
- password: value from `APP_BOOTSTRAP_ADMIN_PASSWORD`

These values can be changed in `src/main/resources/application.yaml`.

## PostgreSQL configuration

The backend reads database settings from Spring environment variables and falls back to local defaults:

- URL: `jdbc:postgresql://localhost:5432/turniring`
- Username: `turniring`
- Password: value from `POSTGRES_PASSWORD`

For Docker Compose these are injected automatically so the backend connects to the `postgres` service.

## Main API groups

- `POST /api/auth/registration`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

- `GET /api/public/home`
- `GET /api/public/tournaments`
- `GET /api/public/tournaments/{id}`
- `GET /api/public/tournaments/{id}/teams`
- `GET /api/public/tournaments/{id}/tasks`
- `GET /api/public/tournaments/{id}/announcements`
- `GET /api/public/tournaments/{id}/schedule`
- `GET /api/public/tournaments/{id}/leaderboard`
- `GET /api/public/invites/{token}`

- `POST /api/admin/tournaments`
- `PUT /api/admin/tournaments/{id}`
- `POST /api/admin/tournaments/{id}/status/{status}`
- `POST /api/admin/tournaments/{id}/tasks`
- `POST /api/admin/tasks/{id}/status/{status}`
- `POST /api/admin/tasks/{id}/assignments`
- `POST /api/admin/tasks/{id}/finish-evaluation`
- `GET /api/admin/tournaments/{id}/submissions`
- `GET /api/admin/tournaments/{id}/leaderboard/export`
- `POST /api/admin/tournaments/{id}/announcements`
- `POST /api/admin/tournaments/{id}/schedule`
- `POST /api/admin/users` (TEAM/ORGANIZER/ADMIN only)
- `GET /api/admin/tournaments/{id}/teams`
- `POST /api/admin/invites/jury`
- `POST /api/admin/invites/teams/{teamId}`

- `POST /api/team/teams`
- `POST /api/team/teams/{teamId}/join/{tournamentId}`
- `POST /api/team/teams/{teamId}/leave`
- `PUT /api/team/teams/{id}`
- `GET /api/team/teams/my`
- `GET /api/team/tournaments/{id}/tasks`
- `PUT /api/team/tasks/{id}/submission`
- `GET /api/team/tasks/{id}/submission`

- `GET /api/jury/assignments`
- `POST /api/jury/assignments/{id}/evaluation`

- `GET /api/profile/me`
- `PUT /api/profile/me`
- `PUT /api/profile/me/role`
- `POST /api/profile/invites/{token}/accept`

## Swagger

Swagger UI is available through Springdoc. Login or registration returns JWT in the response body and also stores it in the `jwt` cookie. Swagger can use bearer auth through the `Authorize` button with:

```text
Bearer <token>
```

## Status model

- Tournament: `DRAFT`, `REGISTRATION`, `RUNNING`, `FINISHED`
- Task: `DRAFT`, `ACTIVE`, `SUBMISSION_CLOSED`, `EVALUATED`

## Leaderboard formula

Each submission score is the average of all jury totals for that submission. Tournament score is the sum of task averages across all tasks in the tournament.
