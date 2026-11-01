# AUM Project Hub

[![CI](https://github.com/DShivaram01/aum-project-hub/actions/workflows/ci.yml/badge.svg)](https://github.com/DShivaram01/aum-project-hub/actions/workflows/ci.yml)

At AUM there isn't a simple place where faculty can post a research or side project and students can ask to join. It usually happens over email or by word of mouth. I built this to fix that: faculty and students post projects, other people browse and filter them by skill, send a join request with a short message, and the project owner accepts or rejects it.

**Live demo:** http://aum-pr-LoadB-JuZAyVVHlFOR-1194787445.us-east-1.elb.amazonaws.com

The home page has one-click logins for a demo faculty account and a demo student account (password `demo1234`), so you can try both sides without registering. The demo is plain HTTP, so please don't reuse a real password if you register.

## What it does

- Register as a student or faculty member and log in (JWT)
- Post projects, edit or close them, and delete them
- Browse projects and filter by category, status, or skill
- Send a join request to someone else's project, and withdraw it while it's still pending
- As the owner, see the requests for your project and accept or reject them
- Edit your own profile (department, skills, short bio)

A few rules are enforced on the backend, not just in the UI: you can't request to join your own project, closed projects don't take requests, you can only send one request per project, and once a request is accepted or rejected it can't be changed again.

## Tech stack

- **Backend:** Java 17, Spring Boot 3.5, Spring Security with JWT, Spring Data JPA
- **Database:** PostgreSQL (H2 in tests)
- **Frontend:** plain HTML, CSS and JavaScript served by Spring Boot
- **Testing:** JUnit 5, Mockito, MockMvc (68 tests)
- **DevOps:** Docker, Docker Compose, GitHub Actions
- **AWS:** ECS Fargate, Application Load Balancer, RDS PostgreSQL, Secrets Manager, CloudWatch, CloudFormation

## How it's put together

```text
src/main/java/com/aum/projecthub/
├── config/       security setup, JWT, request logging
├── controller/   REST endpoints
├── service/      business rules (ownership checks, join request rules)
├── repository/   Spring Data JPA repositories
├── model/        JPA entities: User, Project, JoinRequest
├── dto/          request/response objects, so entities and password hashes never leave the API
├── exception/    a few exceptions mapped to 400/403/404/409 in one handler
└── demo/         seeds demo accounts and projects when app.demo-data=true
src/main/resources/static/   the frontend
infra/aws/                   CloudFormation templates
```

It's a standard layered Spring Boot app. Controllers stay thin, services hold the rules, and repositories talk to the database. The package name follows the usual Java convention of a reversed domain plus the project name.

There are three tables: `users`, `projects` (each has an owner) and `join_requests` (links a user to a project, with a status). `join_requests` has a unique constraint on `(project_id, user_id)`. The service checks for duplicates first, but two requests that arrive at the same moment can both pass that check, so the constraint is the real guarantee. When it fires, the API returns 409 instead of a 500.

### Login

Passwords are hashed with BCrypt. Logging in returns a JWT that's valid for 24 hours, and the frontend sends it as `Authorization: Bearer <token>`. A filter checks the token on every request, so there are no server-side sessions. Passwords are capped at 72 characters because BCrypt ignores anything after the first 72 bytes.

## API

| Method | Endpoint | Who | What it does |
|--------|----------|-----|--------------|
| POST | `/api/auth/register` | anyone | create an account |
| POST | `/api/auth/login` | anyone | get a JWT |
| GET | `/api/users/me` | logged in | your profile |
| GET | `/api/users/{id}` | logged in | someone's profile |
| PUT | `/api/users/{id}` | that user | update your profile |
| GET | `/api/projects` | anyone | list projects, optional `category`, `status`, `skill` filters |
| GET | `/api/projects/{id}` | anyone | project details |
| POST | `/api/projects` | logged in | create a project |
| PUT | `/api/projects/{id}` | owner | update a project |
| DELETE | `/api/projects/{id}` | owner | delete a project and its requests |
| POST | `/api/projects/{id}/join-requests` | logged in | ask to join |
| GET | `/api/projects/{id}/join-requests` | owner | requests for your project |
| GET | `/api/join-requests/mine` | logged in | requests you've sent |
| PUT | `/api/join-requests/{id}` | owner | accept or reject |
| DELETE | `/api/join-requests/{id}` | requester | withdraw a pending request |
| GET | `/actuator/health` | anyone | health check, includes the database |

Errors come back as `{"error": "..."}`, or as a field-to-message map for validation errors. Status codes: 400 bad input, 401 not logged in, 403 not allowed, 404 not found, 409 conflict.

## Running it locally

You need Docker. To run the app and PostgreSQL together:

```bash
docker compose up --build
```

Then open http://localhost:8080. Demo data is turned on in the compose file, so the demo accounts work locally too.

To run the app from your IDE or with Maven instead (needs Java 17):

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Configuration comes from environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 characters), and `APP_DEMODATA=true` if you want the demo data.

## Tests

```bash
./mvnw test
```

- Repository tests with `@DataJpaTest` cover the custom search query and the database constraints.
- Service tests with Mockito cover the business rules, including every join request rule.
- Integration tests with MockMvc start the whole app on H2 and go through real HTTP flows, including the security cases (401 without a token, 403 for someone else's project) and the error handling.

## CI/CD and AWS

GitHub Actions runs on every push:

1. `./mvnw verify`
2. Builds the Docker image, starts it with PostgreSQL using Docker Compose, and runs a quick register/login/create-project check against it
3. Lints the CloudFormation templates
4. On `main`, pushes the image to Amazon ECR and rolls it out to ECS. If the new version fails its health checks, ECS rolls back automatically.

GitHub authenticates to AWS with OIDC, so there are no AWS keys stored in the repo.

On AWS the app runs as a container on ECS Fargate behind an Application Load Balancer. The data lives in an RDS PostgreSQL instance that only the app can reach, with encrypted storage and 7 days of automatic backups. The database password and the JWT secret are kept in Secrets Manager and injected when the container starts. Everything is defined in `infra/aws/`:

- `bootstrap.yml` creates the ECR repository and the role GitHub Actions uses.
- `app.yml` creates the network rules, database, load balancer, ECS service, and monitoring.

For monitoring, the app logs one JSON line per API request with the method, path, status, time taken and a request id. Every response also carries that id in an `X-Request-Id` header. CloudWatch turns these logs into a dashboard showing p50/p95/p99 latency and the slowest endpoints, and alarms send an email on 5xx errors, slow responses, failing health checks, or high database CPU.

## Things I'd do next

- Add HTTPS with a custom domain
- Replace `ddl-auto=update` with Flyway migrations
- Add pagination to the project list
- Notify project owners by email when someone requests to join
