# Analyzer

Spring Boot backend for vacancy and resume analysis.

## Local development

Start PostgreSQL and Redis in Docker:

```bash
docker compose -f compose.local.yml up -d
```

Then run the backend directly from IntelliJ IDEA with `SPRING_PROFILES_ACTIVE=local`, or with Gradle:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

The local profile connects to PostgreSQL at `localhost:5432` and Redis at `localhost:6379`. Its default database name,
username, and password are all `analyzer`. If you override the `POSTGRES_*` values used by Docker, pass matching
`SPRING_DATASOURCE_*` values to the backend. Keep `POLZA_API_KEY` and other secrets in environment variables, an IDE
Run Configuration, or a local `.env` file; do not commit them.

## Codex CLI resume analysis provider

The optional `CODEX_CLI` provider requires OpenAI Codex CLI to be installed and authenticated for the operating-system
account running the Spring process. The configured executable must be available to that process. Enable it with
`CODEX_ENABLED=true`; optionally set `CODEX_EXECUTABLE`, `CODEX_MODEL`, `CODEX_TIMEOUT`, and
`CODEX_MAX_CONCURRENT_PROCESSES`.

Codex authentication is server-side only. API keys, login sessions, `CODEX_HOME`, executable paths, and CLI flags are
never accepted from the frontend. Resume analysis uses `codex exec` non-interactively with a read-only sandbox,
structured output, an isolated temporary working directory, and the prompt over stdin.

Use `provider=POLZA` or `provider=CODEX_CLI` on `POST /api/resume/analyze`. If omitted, the backend uses
`AI_DEFAULT_PROVIDER` (default `POLZA`). `GET /api/ai/providers` reports enabled and compatible providers without
exposing authentication or filesystem details. Availability verifies the executable and required CLI flags; actual
authentication errors are reported only when an analysis is attempted.
