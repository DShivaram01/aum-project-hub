# AUM Project Hub

A web app where AUM students and faculty can post projects, browse them, and request to join.
Project owners can accept or reject join requests.

**Stack:** Java 17, Spring Boot 3, Spring Data JPA, PostgreSQL, JUnit 5

## Status

- [x] Phase 1 – Spring Boot setup, PostgreSQL config, entities, repositories, repository tests
- [ ] Phase 2 – Registration/login (Spring Security), profile endpoints, project CRUD
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

## Project Layout

```text
src/main/java/com/aum/hatchery/
├── controller/   REST endpoints
├── model/        JPA entities and enums
└── repository/   Spring Data JPA repositories
```

`service/`, `dto/`, `config/`, and `exception/` get added in Phase 2 and 3 when they have real code in them.

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

Database settings can be changed with the `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment variables.

## Running Tests

```bash
./mvnw test
```

Tests use an in-memory H2 database, so PostgreSQL does not need to be running.
