# AUM Project Hub

[![CI](https://github.com/DShivaram01/aum-project-hub/actions/workflows/ci.yml/badge.svg)](https://github.com/DShivaram01/aum-project-hub/actions/workflows/ci.yml)

A web app where AUM students and faculty can post projects, browse them, and request to join.
Project owners review join requests and accept or reject them.

**Stack:** Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL, JUnit 5, Mockito,
Docker, GitHub Actions, AWS (ECS Fargate, RDS, CloudWatch, CloudFormation)

## Status

- [x] Phase 1 – Spring Boot setup, PostgreSQL config, entities, repositories, repository tests
- [x] Phase 2 – Registration/login (Spring Security + JWT), profile endpoints, project CRUD
- [x] Phase 3 – Join-request workflow, owner-only authorization, validation and error handling
- [x] Phase 4 – Service/controller tests, Docker, GitHub Actions CI
- [x] Phase 5 – Simple frontend, monitoring, AWS deployment pipeline ([docs/DEPLOYMENT.md](docs/DEPLOYMENT.md))

## Architecture

```text
Browser (static HTML/JS served by Spring Boot)
   |  JSON over HTTP, Authorization: Bearer <JWT>
   v
RequestLoggingFilter -> JwtAuthFilter -> Spring Security rules
   |
Controllers -> Services (ownership + business rules) -> Spring Data JPA -> PostgreSQL
```

On AWS: Application Load Balancer -> ECS Fargate -> RDS PostgreSQL, secrets in Secrets Manager,
JSON logs, alarms and a latency dashboard in CloudWatch. All of it is defined in CloudFormation
(`infra/aws/`) and deployed from GitHub Actions. See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Data Model

| Table           | Main fields                                                                 |
|-----------------|-----------------------------------------------------------------------------|
| `users`         | name, email (unique), password (hashed), role (STUDENT/FACULTY), department, skills, bio |
| `projects`      | title, description, category, requiredSkills, owner_id → users, status (OPEN/CLOSED), createdAt |
| `join_requests` | project_id → projects, user_id → users, message, status (PENDING/ACCEPTED/REJECTED), createdAt |

- A user can own many projects.
- A project can have many join requests.
- `join_requests` has a unique constraint on `(project_id, user_id)` so a user can only request a project once.
  The service checks this first and returns `409`; if two requests race past that check, the database
  constraint rejects the second one and it is also mapped to `409`.

## API

| Method | Endpoint                              | Auth             | Description                                   |
|--------|---------------------------------------|------------------|-----------------------------------------------|
| POST   | `/api/auth/register`                  | none             | Create a STUDENT or FACULTY account           |
| POST   | `/api/auth/login`                     | none             | Returns a JWT token                           |
| GET    | `/api/users/me`                       | token            | Logged-in user's profile                      |
| GET    | `/api/users/{id}`                     | token            | View a profile                                |
| PUT    | `/api/users/{id}`                     | token, self      | Update name, department, skills, bio          |
| GET    | `/api/projects`                       | none             | Browse; optional `category`, `status`, `skill` filters |
| GET    | `/api/projects/{id}`                  | none             | Project details                               |
| POST   | `/api/projects`                       | token            | Create a project (you become the owner)       |
| PUT    | `/api/projects/{id}`                  | token, owner     | Update a project                              |
| DELETE | `/api/projects/{id}`                  | token, owner     | Delete a project and its join requests        |
| POST   | `/api/projects/{id}/join-requests`    | token            | Ask to join, with an optional message         |
| GET    | `/api/projects/{id}/join-requests`    | token, owner     | The owner's list of requests for a project    |
| GET    | `/api/join-requests/mine`             | token            | Requests you have sent                        |
| PUT    | `/api/join-requests/{id}`             | token, owner     | `{"status": "ACCEPTED"}` or `"REJECTED"`      |
| DELETE | `/api/join-requests/{id}`             | token, requester | Withdraw a pending request                    |
| GET    | `/actuator/health`                    | none             | Health check including the database           |

Join-request rules: you cannot request your own project, the project must be `OPEN`,
only `PENDING` requests can be accepted, rejected or withdrawn.

Errors are returned as `{"error": "..."}` (or `{"field": "message"}` for validation errors):
`400` invalid input, `401` not logged in / bad login, `403` not allowed, `404` not found,
`409` duplicate email, duplicate join request, or request already decided.

## How Login Works

1. `POST /api/auth/login` checks the password against the BCrypt hash in the database.
2. If it matches, the server returns a signed JWT token that holds the user's email and expires after 24 hours.
3. The client sends `Authorization: Bearer <token>` on later requests.
4. `JwtAuthFilter` checks the token on every request and tells Spring Security who the user is.

No sessions are stored on the server. Passwords are limited to 72 characters because BCrypt
only uses the first 72 bytes.

## Monitoring

- `/actuator/health` checks the app and its database connection; the load balancer uses it.
- `RequestLoggingFilter` logs every API call with method, path, status and latency,
  at WARN for 4xx and ERROR for 5xx.
- Every response has an `X-Request-Id` header, and the same id is attached to every log line for that request.
- With the `aws` profile logs are JSON, so CloudWatch can chart p95 latency per endpoint and alarm on errors.

## Project Layout

```text
src/main/java/com/aum/hatchery/
├── config/       Security setup, JWT creation and checking, request logging
├── controller/   REST endpoints
├── dto/          Request/response objects (so entities and passwords are never sent directly)
├── exception/    Custom exceptions and the global error handler
├── model/        JPA entities and enums
├── repository/   Spring Data JPA repositories
└── service/      Business logic (ownership checks, join-request rules, password hashing)
src/main/resources/static/   Frontend (plain HTML, CSS, JavaScript)
infra/aws/                   CloudFormation templates
.github/workflows/ci.yml     Tests, Docker smoke test, template lint, deploy
```

## Running Locally

Requirements: Java 17+, Docker.

Everything in Docker:

```bash
docker compose up --build
# open http://localhost:8080
```

Or run only PostgreSQL in Docker and the app with Maven:

```bash
docker compose up -d postgres
./mvnw spring-boot:run
curl http://localhost:8080/actuator/health
```

Example session:

```bash
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@aum.edu","password":"password123","role":"STUDENT"}'

TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"alice@aum.edu","password":"password123"}' | jq -r .token)

curl -X POST localhost:8080/api/projects -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Grade Tracker","description":"Track course grades","category":"Web","requiredSkills":"Java, Spring Boot"}'

curl "localhost:8080/api/projects?skill=java&status=OPEN"

curl -X POST localhost:8080/api/projects/1/join-requests -H "Authorization: Bearer $OTHER_TOKEN" \
  -H "Content-Type: application/json" -d '{"message":"I have used Spring Boot before"}'
```

Settings can be changed with environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`
(always set your own `JWT_SECRET`, at least 32 characters, outside local development).

## Running Tests

```bash
./mvnw test
```

- **Repository tests** (`@DataJpaTest`): queries and database constraints.
- **Service tests** (JUnit + Mockito): business rules such as password hashing, duplicate emails,
  owner-only updates and every join-request rule, with the database mocked out.
- **API integration tests** (`@SpringBootTest` + MockMvc): full request flow including security,
  e.g. 401 without a token, 403 for non-owners, 409 for duplicate join requests, 400 for bad input.
- **Monitoring and frontend tests**: health endpoint, request ids, request logging, static files.

Tests use an in-memory H2 database, so PostgreSQL does not need to be running.

## CI/CD

GitHub Actions runs on every push and pull request:

1. `./mvnw verify` (all tests)
2. Builds the Docker image, starts it with PostgreSQL through Docker Compose, and smoke-tests
   register → login → create project
3. Lints the CloudFormation templates
4. On `main`, once AWS is set up: pushes the image to ECR and rolls it out on ECS, with automatic
   rollback if the new version fails its health checks
