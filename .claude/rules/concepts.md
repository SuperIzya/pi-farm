# Core Concepts

## Domain Model

### Controller
A physical IoT device deployed in the field (e.g., an Arduino board). Has an `id`, `typeId` (links to `ControllerType`), `name`, `description`, and optional sparse `presentation` JSON keyed by periphery name and channel name. Controller overrides are applied per presentation field.

### ControllerType
A hardware blueprint — same board model + wiring layout. Defines the peripheries map (`Map[PeripheryName, PeripheryTypeId]`) describing what sensor/actuator is attached to each pin/port. Also holds an optional `schema` URL and optional sparse `presentation` JSON keyed by periphery name and channel name. Its PlatformIO source project is stored as `<code-dir>/<id>.tar.gz`, not in the database.

### PeripheryType
A type of sensor or actuator (e.g., "DHT22 Humidity Sensor", "Solenoid Motor"). Has an `image` (data URL or static resource path) and `connections: NonEmptyChunk[Connection]` — the data channels it exposes, each with a `name`, `direction` (In/Out/Both), `units`, data `type`, and nullable `presentation` JSON for that channel. Controller-type and controller presentation fields can override channel defaults.

### Address
A fully-qualified pointer to a specific data channel on a specific controller: `controllerId` + `peripheryName` + `peripheryChannel` + `processorConnectionName`. Used to bind processor inlets/outlets to physical hardware endpoints.

### FlowConfiguration
A deployable data-flow pipeline. Contains `graphData` (JSON for the UI graph layout), and one or more `Processor` entries, each specifying which `DataProcessor` to run (`unit`), its runtime `parameters` (JSON), named `inbound`/`outbound` `Address` bindings, and a `graphId` (unique identifier for the UI node graph). `FlowConfigurationManager` requires every expected inlet and outlet exactly once and validates hardware compatibility before saving. Can be started/stopped without restarting the server.

### ProcessorDefinition
Metadata describing a processor's input/output signature — its name, description, JSON Schema for parameters, and ordered lists of `InputConnection`/`OutputConnection`.

### Message (ADT)
The universal message format, split into `Inbound` (from controllers: `DataPacket`, `Measurements`, `Error`, `Discovery`, `Ping`) and `Outbound` (to controllers: `DataPacket`, `Command`, `ServerDiscovered`, `Pong`). `ServerUp` belongs to both.

---

## Plugin System

### DataProcessor
The core trait every processing plugin implements. Defines `ParamsType`, its JSON codec, a `work: ConfigurableFlow` method (the actual logic), and auto-generated `processorDefinition`.

### Manifest
Discovery entry-point for a plugin module. Has `version`, `name`, and registers all `processors` and `services` it provides. Each plugin module has exactly one `Manifest` object.

### Service
A singleton signal processor for system-wide message routing (not bound to a specific configuration). The `Service` trait exposes a `service: Service.Creator` method. `Service.Creator` is `Init[Worker]` (i.e., `RIO[Environment, Worker]`). `Worker` has a `serviceName: Name` and `transform: SignalStream => Task[ResponseStream]`. Examples: Discovery handling, PingPong, Heartbeat.

### Inlet[In] / Outlet[Out]
Typed input/output port definitions for processors. `Inlet` parses JSON → typed value; `Outlet` formats typed value → JSON.

### Plugin DSL (Flow/Source/Sink/ConfigurableFlow)
Builder DSL for defining processor logic: `from(inlets).to(outlets).via(transformFunction)` produces a `ConfigurableFlow` that can be configured with a specific `FlowConfiguration.Processor` to yield a `ZPipeline[Inbound, Outbound]`.

### @processor macro
Auto-generates `processorDefinition` by introspecting `Inlet`/`Outlet` vals and `ParamsType`.

---

## Processing Engine

### Factory
Runtime engine that starts services and flow configurations. Subscribes to `SignalHub` for each service/processor pipeline, pipes output into `ResponseQueue`. Processor scopes are keyed by configuration ID, unit name, and graph ID. Updating a configuration closes its previous processor scopes before starting replacements; deleting it closes all its processor scopes.

### FlowConfigurationChanges
Wraps `ConfigurationRepository` and a scoped change queue. Emits stored configurations at startup, then add, update, and delete events for the Factory.

### Discovery (service)
Handles controller self-registration: validates the controller exists in DB, registers in the `Controllers` runtime registry, responds with `ServerDiscovered`.

---

## Runtime Primitives

### Controllers (registry)
In-memory bidirectional map: network address ↔ Controller. Populated by Discovery, used by SignalStream (address→controller) and OutboundRawStream (controller→address). Both indexes update atomically; moving a controller or reassigning its address removes obsolete mappings.

### SignalHub / ResponseHub
`StreamHub` implementations providing scoped stream subscriptions via `subscribe: URIO[Scope, ZStream]`. `SignalHub` broadcasts inbound messages to all processors/services; `ResponseHub` drains `ResponseQueue` and broadcasts outbound messages for subscribers like the WebSocket layer.

### ResponseQueue
A plain `Queue[Outbound]` where Factory (processors and services) enqueue outbound messages. `ResponseHub` drains this queue; `ResponseStream` is the stream type alias used by its subscribers.

### SignalStream
A ZStream built from the UDP inbound queue (`Queues.inbound`), parsing `RawMessage` → `Message.Inbound` with controller validation. Controller IDs on messages from registered senders must match their registration, except Discovery, which can reassign an address. Unregistered senders may send Discovery, Error, or ServerUp. Provided as a ZLayer; `SignalHub` broadcasts it to multiple subscribers.

### UIIncomingQueue / UIIncomingHub
Queue and hub for data packets sent from the UI to the processing pipeline (e.g., manual actuator commands).

### OrphanCleanup
Application-scoped worker using `StaticService.Config` for code, images, and staging directories.
`codeDir` and `imageDir` are relative to `baseDir`; image references remain relative to that root.
Export bundles retain the portable `images/` namespace independently of the configured image directory.
Scans immediately and waits 24 hours after each completed cycle. Per-folder cleanup tasks lazily scan
managed code archives and images sequentially, skipping entries modified within the last 24 hours
and deleting only after parameterized repository existence checks confirm
there is no reference. A shared publication lock protects image saves/imports through database
commit from cleanup's check-and-delete. Filesystem work uses one low-priority executor, while
repository queries keep their existing database executor. Staging entries older than 24 hours
are removed, including whole directory trees based on the top-level directory's timestamp,
without following symlinks. Shutdown interrupts the worker and closes directory streams and executor.

### ControllerTypeManager
Coordinates controller-type creation, updates, deletion, and imports with source archives.
New types, including imports, require valid firmware; metadata-only updates preserve existing code.
Validation and canonical staging happen before DB writes. Creation and periphery relations commit
in one transaction, followed by atomic archive publication on the same filesystem. The shared
StaticService publication lock serializes mutations with orphan cleanup; replacements use
last-successful-publication-wins semantics without revision checks. Deletion commits the DB change first,
then attempts to remove firmware. File-deletion failures are logged but do not fail the operation;
orphan cleanup retries leftover archives. Publication failures still fail explicitly, and missing code
can be repaired by replacement. This is in-process
coordination, not a cross-process lock or a database/filesystem transaction.

---

## Communication

### UDP (server ↔ microcontrollers)
Netty-based NIO DatagramChannel. Pipeline: `DatagramPacket → BinaryMessage → RawMessage → SignalStream → SignalHub` (inbound) and `ResponseQueue → ResponseHub → OutboundRawStream → RawMessage → UDP` (outbound). `Queues` (trait in `common`) and `RawMessage` are shared between server and common modules.

### WebSocket (server ↔ UI)
Typed Command/Data ADTs with kebab-case discriminators. Large messages are split into partial chunks (32KB boundary). WSProcessor dispatches commands to repositories/managers.

---

## Common Plugins

- **PlantWatering** — automated irrigation based on humidity thresholds
- **HumidityVentilation** — controls ventilation based on humidity
- **PingPong** — responds to `Ping` with `Pong` (connectivity check)
- **Heartbeat** — sends `Pong` to all registered controllers on startup

---

## Data Flow Diagram

```
PeripheryType ←── referenced by ──→ ControllerType.peripheries
                                          ↑
                                    typeId │
                                     Controller ←── controllerId ──→ Address
                                          ↑                              ↑
                              Discovery   │                   FlowConfiguration.Processor
                              registers   │                        (inbound/outbound)
                                          ↓                              │ unit name
                                    Controllers                          ↓
                                    (runtime registry)           ProcessorDefinition
                                          ↑                              ↑
 UDP ←→ SignalStream → SignalHub → Factory ← FlowConfigurationChanges  DataProcessor
                                       ↓                            (plugin impl)
                              ZPipeline (configured)                     ↑
                                       ↓                            Manifest
                              ResponseQueue → ResponseHub → OutboundRawStream → UDP
                                                    ↓
                                             HttpServer/WS → UI
```
