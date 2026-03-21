---
apply: always
---

## Project Overview
Pi-Farm is an IoT control platform for Raspberry Pi and microcontrollers, written in Scala 3 with ZIO 2. It connects to devices via UDP, manages configurations, and supports a pluggable processor/driver architecture.

## Language & Compiler
- Use **Scala 3** syntax exclusively. Never use Scala 2-style syntax.
- The project compiles with `-experimental` enabled — Scala 3 experimental features are allowed.
- `scalafmtOnCompile` is enabled; keep code formatted consistently with the existing style.

## Effect System — ZIO 2
- All effects must use ZIO 2 types: `ZIO[R, E, A]`, `RIO[R, A]`, `Task[A]`, `UIO[A]`, `URIO[R, A]`.
- Use `ZLayer` for dependency injection. Follow the existing pattern of a companion object with a `live` ZLayer definition.
- Use `Scope` and `ZIO.scoped` for resource lifecycle management.
- Prefer `ZIO.serviceWithZIO` / `ZIO.serviceWith` to access services from the environment.
- Use `ZStream` for streaming pipelines (inbound/outbound message flows).

## Architecture Conventions
- **Module structure**: `common` (domain models, storage, plugin traits), `server` (HTTP, UDP, processing), `common-plugins` (built-in plugin implementations).
- **Repository pattern**: Data access classes end in `Repository`; implement as a trait + `live` ZLayer object.
- **Plugin architecture**: Plugins implement `Manifest` and register `Processor`s and services. Do not hardcode driver logic in the server module.
- **Message hierarchy**: All inter-device messages extend the sealed `Message` trait. Add new message types there, not ad-hoc.

## Type Safety
- Use **opaque types** (e.g. `ControllerId`, `PeripheryId`, `ConfigurationId`, `Name`) rather than raw primitives for domain identifiers.
- Use `Inlet[A]` / `Outlet[A]` with `given` instances for type-safe stream connections.
- Use `DeriveJsonCodec` from zio-json for JSON codecs. Do not write manual codecs unless necessary.
- Use Chimney for data transformations between model layers instead of manual mapping.

## HTTP / JSON
- HTTP is handled via **zio-http 3.x**. Use its `Routes`, `Handler`, and `Response` APIs.
- JSON serialization uses **zio-json**. All request/response types need a `JsonCodec` (via `DeriveJsonCodec.gen`).

## Database
- Use **Doobie** with the ZIO interop layer for all SQL access.
- The SQL dialect is **H2**. Write SQL compatible with H2; avoid PostgreSQL/MySQL-specific syntax.
- All schema changes go through **Flyway** migration scripts under `src/main/resources/db/migration`.
- Never issue DDL from application code at runtime.

## Configuration
- Application config uses **zio-config** with HOCON/typesafe-config. Add new config sections as case classes with a `ZLayer` descriptor.

## Testing
- Use the **ZIO Test** framework (`ZIOSpecDefault`, `suite`, `test`, `assertTrue`).
- Fork tests (`test / fork := true`) — do not rely on shared JVM state between test suites.

## Naming
- Package root: `org.pi.farm`
- Service traits: noun (e.g. `ControllerRegistry`); implementations: companion object with `live` layer.
- Avoid abbreviations in names unless already established in the codebase (e.g. `Udp`, `Http`).

## General
- Prefer functional, immutable data transformations. Avoid `var` and mutable state outside ZIO `Ref`.
- Do not add `println` or raw `System.out` logging; use ZIO logging (`ZIO.logInfo`, `ZIO.logError`, etc.).
- Keep the `common` module free of server-specific dependencies. Domain models and storage belong there; HTTP routes and UDP handling belong in `server`.

## Response Style
- Always provide concise, functional code first.
- Use the **1-3-1 rule** for complex problems: 1 problem statement, 3 solutions, 1 recommendation.
- Prioritize minimal context to reduce latency.
