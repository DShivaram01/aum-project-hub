# AUM Project Hub

A web app where AUM students and faculty can post projects, browse them, and request to join.
Project owners can accept or reject join requests.

**Stack:** Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL, JUnit 5, Mockito

## Status

- [x] Phase 1 – Spring Boot setup, PostgreSQL config, entities, repositories, repository tests
- [x] Phase 2 – Registration/login (Spring Security + JWT), profile endpoints, project CRUD
- [ ] Phase 3 – Join-request workflow, owner-only authorization, validation and error handling
- [ ] Phase 4 – Service/controller tests, Docker, GitHub Actions CI
- [ ] Phase 5 – Simple frontend, AWS deployment

## Data Model

| Table           | Main fields                                                                 |
|-----------------|-----------------------------------------------------------------------------|
| `users`         | name, email (unique), password (hashed), role (STUDENT/FACULTY), department, skills, bio |
| `projects`      | title, description, category, requiredSkills, owner_id → users, status (OPEN/CLOSED), createdAt |
| `join_requests` | project_id → projects, user_id → users, message, status (PENDING/ACCEPTED/REJECTED), createdAt |

- A user can own many projects.
- A project can have many join requests.
- `join_requests` has a unique constraint on `(project_id, user_id)` so a user can only request a project once.

## API

| Method | Endpoint                | Auth        | Description                                   |
|--------|-------------------------|-------------|-----------------------------------------------|
| POST   | `/api/auth/register`    | none        | Create a STUDENT or FACULTY account           |
| POST   | `/api/auth/login`       | none        | Returns a JWT token                           |
| GET    | `/api/users/me`         | token       | Logged-in user's profile                      |
| GET    | `/api/users/{id}`       | token       | View a profile                                |
| PUT    | `/api/users/{id}`       | token, self | Update name, department, skills, bio          |
| GET    | `/api/projects`         | none        | Browse; optional `category`, `status`, `skill` filters |
| GET    | `/api/projects/{id}`    | none        | Project details                               |
| POST   | `/api/projects`         | token       | Create a project (you become the owner)       |
| PUT    | `/api/projects/{id}`    | token, owner| Update a project                              |
| DELETE | `/api/projects/{id}`    | token, owner| Delete a project                              |

Errors: `400` invalid input, `401` not logged in / bad login, `403` not the owner, `404` not found, `409` email already registered.

## How Login Works

1. `POST /api/auth/login` checks the password against the BCrypt hash in the database.
2. If it matches, the server returns a signed JWT token that holds the user's email and expires after 24 hours.
3. The client sends `Authorization: Bearer <token>` on later requests.
4. `JwtAuthFilter` checks the token on every request and tells Spring Security who the user is.

No sessions are stored on the server.

## Project Layout

```text
src/main/java/com/aum/hatchery/
├── config/       Security setup, JWT creation and checking
├── controller/   REST endpoints
├── dto/          Request/response objects (so entities and passwords are never sent directly)
├── exception/    Custom exceptions and the global error handler
├── model/        JPA entities and enums
├── repository/   Spring Data JPA repositories
└── service/      Business logic (ownership checks, password hashing, etc.)
```

## Running Locally

Requirements: Java 17+, Docker (for PostgreSQL).

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Start the app
./mvnw spring-boot:run

# 3. Check it is running
curl http://localhost:8080/api/health
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
```

Settings can be changed with environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`
(always set your own `JWT_SECRET`, at least 32 characters, outside local development).

## Running Tests

```bash
./mvnw test
```

- **Repository tests** (`@DataJpaTest`): queries and database constraints.
- **Service tests** (JUnit + Mockito): business rules such as password hashing, duplicate emails, and owner-only updates, with the database mocked out.
- **API integration tests** (`@SpringBootTest` + MockMvc): full request flow including security, e.g. 401 without a token and 403 for non-owners.

Tests use an in-memory H2 database, so PostgreSQL does not need to be running.
