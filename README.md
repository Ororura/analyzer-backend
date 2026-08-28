# Analyzer

Spring Boot backend for vacancy and resume analysis.

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
