---
apply: always
---

## Model Classes

Model classes live in `modules/common/src/main/scala/org/pi/farm/model/`.

### When modifying model classes

1. **Always ask for clarification first.** Before changing any model class, ask the user to explain the intent and any domain details that may not be obvious from the types alone (e.g. what a field stores, its format, units, invariants).

2. **Keep scaladoc up to date.** After every change to a model class, update (or add) the scaladoc comment on the affected class and its fields to reflect the new structure. Pay particular attention to non-obvious semantics — data formats, external references, ordering constraints, and valid value ranges.

### Key domain notes

- `PeripheryType.image` — data URL (e.g. `data:image/png;base64,...`), not a file path or CDN URL
- `ControllerType.schema` — URL to a soldering/wiring schema file, not a JSON Schema
- `ControllerType.peripheries: Map[PeripheryId, PeripheryTypeId]` — maps pin/port identifiers (e.g. `"1-3"`) to the type of periphery attached there
- `Configuration.inbound` / `Configuration.outbound` — positional lists; must align with the processing unit's channel lists in order
- `ProcessingUnit.outbound` — a list; a unit can emit multiple values of different types (e.g. a `Boolean` flag and a `Float` angle)
