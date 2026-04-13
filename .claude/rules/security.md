# Security Guardrails

## Secrets Management

- **Never hardcode** credentials, API keys, database passwords, or connection strings in source code
- Use `application.conf` / `reference.conf` with environment variable overrides for sensitive values
- Use ZIO Config derivation to bind configuration — never parse secrets manually
- Database credentials go through `DbConfig` → `reference.conf` → environment variable override chain
- Do not commit `data/pi-farm.mv.db` with production data to version control

## Input Validation

- All external input (HTTP requests, WebSocket messages, UDP packets) must be validated before processing
- Use ZIO JSON decoders for structured input — they reject malformed payloads by default
- Use ZIO Schema validation for domain model constraints
- Validate all numeric ranges, string lengths, and enum values at the boundary
- Never trust microcontroller input — validate UDP message structure and address ranges

## Database Security

- **Always use parameterized queries** via Doobie's `sql` interpolator — never concatenate user input into SQL strings
- Flyway migrations are the only mechanism for schema changes
- H2 database file permissions should restrict access to the server process user
- Use connection pooling (HikariCP via Doobie) with bounded pool sizes

## WebSocket Security

- Validate all incoming WebSocket `Command` messages against the typed ADT — reject unknown commands
- Sanitize any user-provided strings before broadcasting to other connected clients
- Implement connection limits to prevent resource exhaustion

## HTTP Security

- Serve the UI from compiled static assets only (`modules/server/src/main/resources/ui/`)
- Do not serve arbitrary files from the filesystem
- Set appropriate `Content-Type` headers on all responses
- For production: add CORS headers, rate limiting, and authentication middleware

## Dependency Security

- Pin dependency versions in `project/Dependencies.scala` — no floating version ranges
- Run `sbt dependencyCheck` periodically (if OWASP plugin is added) to scan for CVEs
- Run `npm audit` in `modules/ui/` before releases
- Review transitive dependencies when upgrading major versions
- Keep H2 database version pinned and reviewed (known CVE history)

## Frontend Security

- No `dangerouslySetInnerHTML` unless absolutely necessary and input is sanitized
- Sanitize any data rendered from WebSocket messages
- Use TypeScript strict mode to catch type confusion bugs
- Do not store sensitive data in Redux state or localStorage
- CSP headers should be configured when deploying behind a reverse proxy

## Network Security

- UDP server binds to configured address/port only — do not bind to `0.0.0.0` in production without firewall rules
- HTTP server port should be configurable via `application.conf`
- For production deployment: use TLS termination at a reverse proxy (nginx, Caddy)
- Do not expose H2 console or debug endpoints in production

## File System

- Never write user-supplied content directly to the filesystem
- Plugin discovery scans the classpath — ensure untrusted JARs cannot be placed on the classpath
- Arduino schematics and data files should not be served via the HTTP server
