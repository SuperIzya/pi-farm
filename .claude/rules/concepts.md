# Core Concepts

## Domain Model

### Controller
A physical IoT device deployed in the field (e.g., an Arduino board). Has an `id`, `typeId` (links to `ControllerType`), `name`, and `description`.

### ControllerType
A hardware blueprint — same board model + wiring layout. Defines the peripheries map (`Map[PeripheryId, PeripheryTypeId]`) describing what sensor/actuator is attached to each pin/port. Also holds the firmware `code` and optional `schema` URL.

### PeripheryType
A type of sensor or actuator (e.g., "DHT22 Humidity Sensor", "Solenoid Motor"). Has `connections: NonEmptyChunk[Connection]` — the data channels it exposes, each with a `name`, `direction` (In/Out/Both), `units`, and data `type`.

### Address
A fully-qualified pointer to a specific data channel on a specific controller: `controllerId` + `peripheryId` + connection `name`. Used to bind processor inlets/outlets to physical hardware endpoints.

### FlowConfiguration
A deployable data-flow pipeline. Contains one or more `Processor` entries, each specifying which `DataProcessor` to run (`unit`), its runtime `parameters` (JSON), and ordered `inbound`/`outbound` `Address` bindings. Can be started/stopped without restarting the server.

### ProcessorDefinition
Metadata describing a processor's input/output signature — its name, description, JSON Schema for parameters, and ordered lists of `InputConnection`/`OutputConnection`.

### Message (ADT)
The universal message format, split into `Inbound` (from controllers: `DataPacket`, `Measurement`, `Error`, `Discovery`, `Ping`) and `Outbound` (to controllers: `DataPacket`, `Command`, `ServerDiscovered`, `Pong`).

---

## Plugin System

### DataProcessor
The core trait every processing plugin implements. Defines `ParamsType`, its JSON codec, a `work: ConfigurableFlow` method (the actual logic), and auto-generated `processorDefinition`.

### Manifest
Discovery entry-point for a plugin module. Registers all `processors` and `services` it provides. Each plugin module has exactly one `Manifest` object.

### Service
A singleton signal processor for system-wide message routing (not bound to a specific configuration). Has a `transform: SignalStream => Task[ResponseStream]` method. Examples: Discovery handling, PingPong, Heartbeat.

### Inlet[In] / Outlet[Out]
Typed input/output port definitions for processors. `Inlet` parses JSON → typed value; `Outlet` formats typed value → JSON.

### Plugin DSL (Flow/Source/Sink/ConfigurableFlow)
Builder DSL for defining processor logic: `from(inlets).to(outlets).via(transformFunction)` produces a `ConfigurableFlow` that can be configured with a specific `FlowConfiguration.Processor` to yield a `ZPipeline[Inbound, Outbound]`.

### @processor macro
Auto-generates `processorDefinition` by introspecting `Inlet`/`Outlet` vals and `ParamsType`.

---

## Processing Engine

### Factory
Runtime engine that starts services and flow configurations. Subscribes to `SignalHub` for each service/processor pipeline, pipes output into `ResponseQueue`.

### ConfigurationStorage
In-memory queue + database layer for active configurations. Loads existing configs on startup, enqueues new ones for the Factory.

### Discovery (service)
Handles controller self-registration: validates the controller exists in DB, registers in the `Controllers` runtime registry, responds with `ServerDiscovered`.

---

## Runtime Primitives

### Controllers (registry)
In-memory bidirectional map: IP address ↔ Controller. Populated by Discovery, used by InboundStream (IP→controller) and OutboundStream (controller→IP).

### SignalHub / ResponseHub
`StreamHub` case classes providing scoped stream subscriptions via `subscribe: URIO[Scope, ZStream]`. `SignalHub` broadcasts inbound messages to all processors/services; `ResponseHub` broadcasts the `ResponseStream` (fed by `ResponseQueue`) for subscribers like the WebSocket layer.

### ResponseQueue
A plain `Queue[Outbound]` where Factory (processors and services) enqueue outbound messages. The `ResponseStream` drains this queue, and `ResponseHub` broadcasts it.

### SignalStream
A ZStream built from the UDP inbound queue (`Queues.inbound`), parsing `RawMessage` → `Message.Inbound` with controller validation. Provided as a ZLayer; `SignalHub` broadcasts it to multiple subscribers.

### UIIncomingQueue / UIIncomingHub
Queue and hub for data packets sent from the UI to the processing pipeline (e.g., manual actuator commands).

---

## Communication

### UDP (server ↔ microcontrollers)
Netty-based NIO DatagramChannel. Pipeline: `DatagramPacket → BinaryMessage → RawMessage → SignalStream → SignalHub` (inbound) and `ResponseQueue → ResponseStream → OutboundStream → RawMessage → UDP` (outbound). `Queues` (trait in `common`) and `RawMessage` are shared between server and common modules.

### WebSocket (server ↔ UI)
Typed Command/Data ADTs with kebab-case discriminators. Large messages are split into partial chunks (32KB boundary). WSProcessor dispatches commands to repositories/managers.

---

## Common Plugins

- **PlantWatering** — automated irrigation based on humidity thresholds
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
 UDP ←→ SignalStream → SignalHub → Factory ← ConfigurationStorage  DataProcessor
                                       ↓                            (plugin impl)
                              ZPipeline (configured)                     ↑
                                       ↓                            Manifest
                              ResponseQueue → ResponseStream → OutboundStream → UDP
                                                    ↓
                                             ResponseHub → HttpServer/WS → UI
```
