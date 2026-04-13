# PiFarm — Master Guide

## Project Overview

PiFarm is an IoT platform for controlling servos, valves, and other actuators connected to microcontrollers, based on sensor data from those or other microcontrollers. It features a modular plugin architecture where controllers, processing nodes, and drivers are dynamically discoverable at runtime. The system supports real-time data flow configurations that can be started/stopped without restarting the server.

## Tech Stack

- **Backend**: Scala 3.8.2 on JVM
- **Effect System**: ZIO 2.1.x (ZIO Streams, ZIO HTTP 3.10, ZIO Config, ZIO JSON, ZIO Schema, ZIO Logging)
- **Database**: H2 (embedded), Doobie 1.0.0-RC12 (JDBC layer), Flyway 12.3 (migrations)
- **Interop**: ZIO-Cats interop for Doobie integration
- **Transformations**: Chimney 1.9 for case class conversions
- **Build**: sbt with scalafmt (format-on-compile), sbt-pack (packaging)
- **Frontend**: React 19, TypeScript 5.9, Redux Toolkit, MUI 7, React Router 7, XYFlow (node graph editor)
- **Frontend Build**: Webpack 5, SCSS (Sass), ESLint + Prettier
- **Communication**: WebSocket (server↔UI), UDP (server↔microcontrollers)

## Architecture & Directory Structure

```
modules/
  common/          # Shared domain models, plugin API, storage layer, runtime primitives
    model/         # Domain types: Controller, FlowConfiguration, Message, Direction, etc.
    plugin/        # Plugin API: DataProcessor, Service, Manifest, Inlet/Outlet
    plugin/syntax/ # DSL for building processing flows (Flow, Source, Sink, ConfigurableFlow)
    plugin/macros/ # Scala 3 macros for processor code generation
    storage/       # Repository traits + ZIO live layers (Doobie-based): Configuration, Controller, ControllerType, PeripheryType, ProcessingUnits
    runtime/       # Runtime primitives: Controllers registry, response queues/hubs
    utils/         # ConfigCompanion for typesafe config derivation
  common-plugins/  # Built-in processor plugins (PlantWatering, Heartbeat, PingPong, ToUIService)
  server/          # HTTP + UDP server, WebSocket handler, processing engine
    Main.scala     # ZIOApp entry point — assembles all layers
    HttpServer     # ZIO HTTP routes, serves UI static assets
    udp/           # UDP server for microcontroller communication
    ws/            # WebSocket message handling (Command/Data ADTs, WSProcessor)
    processing/    # Flow processing engine (Factory, ProcessingManager, Discovery)
    service/       # Business logic (ConfigurationManager)
  ui/              # React SPA
    src/client/    # WebSocket client, command/data dispatch
    src/inventory/ # CRUD views: controllers, controller-types, periphery-types, configurations
    src/store/     # Redux root store, listeners, typed hooks
    src/utils/     # Shared components (Loading, Error, NavBar), route definitions, SCSS mixins
```

## Development Commands

### Backend (from project root)
- `sbt compile` — Compile all Scala modules
- `sbt test` — Run all tests (ZIO Test)
- `sbt "server/run"` — Start the server (forked JVM)
- `sbt scalafmtAll` — Format all Scala sources
- `sbt scalafmtCheck` — Check formatting without modifying

### Frontend (from `modules/ui/`)
- `npm run build` — Production build (type-check + webpack)
- `npm run build:dev` — Development build
- `npm run watch` — Webpack watch mode
- `npm start` — Start dev server (tsx server.ts)
- `npm run lint` — ESLint check
- `npm run fix` — ESLint auto-fix
- `npm run type-check` — TypeScript compiler check

## Coding Conventions & Constraints

- Use ZIO layers (ZLayer) for dependency injection — never use runtime reflection or service locators
- Prefer `ZIO.serviceWithZIO` / `ZIO.serviceWith` for accessing services
- All repository operations go through Doobie `ConnectionIO` transacted via ZIO-Cats interop
- Domain models use `derives` for ZIO JSON codecs and ZIO Schema derivation
- Use Chimney `transformInto` for model-to-model conversions
- Plugin processors must extend `DataProcessor` and use the macro-based builder
- Frontend components follow Redux Toolkit slice pattern (actions, selectors, store per feature)
- WebSocket messages use a typed Command/Data ADT — never send raw untyped JSON

## Critical Rules

**IMPORTANT**: Never hardcode database credentials or connection strings — use `reference.conf` / `application.conf` with ZIO Config derivation.

**IMPORTANT**: All database schema changes must go through Flyway migrations in `modules/common/src/main/resources/migrations/`. Never modify the database schema manually.

**IMPORTANT**: The `scalafmtOnCompile := true` setting is active — do not disable it. All Scala code must pass scalafmt.

**IMPORTANT**: Tests use ZIO Test with `ZIOSpecDefault`. Always provide layers via `provideSomeLayer` / `provideSomeShared`. Never use `Unsafe.unsafe` in tests.

**IMPORTANT**: The plugin system discovers manifests at runtime via classpath scanning. Each plugin module must have exactly one object extending `Manifest`.

**IMPORTANT**: Frontend state must go through Redux. Never use local component state for data that other components need.

## Response Style
- Always provide concise, functional code first.
- Use the **1-3-1 rule** for complex problems: 1 problem statement, 3 solutions, 1 recommendation.
- Prioritize minimal context to reduce latency.