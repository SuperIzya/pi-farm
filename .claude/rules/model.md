# Working with model classes

## Scala (Backend)

### Model Classes

Model classes live in `modules/common/src/main/scala/org/pi/farm/model/`.

#### When modifying model classes

1. **Always ask for clarification first.** Before changing any model class, ask the user to explain the intent and any domain details that may not be obvious from the types alone (e.g. what a field stores, its format, units, invariants).

2. **Keep scaladoc up to date.** After every change to a model class, update (or add) the scaladoc comment on the affected class and its fields to reflect the new structure. Pay particular attention to non-obvious semantics — data formats, external references, ordering constraints, and valid value ranges.

## TypeScript (Frontend)

### Model Types

1. Model types for entities live in `modules/ui/src/types/index.ts`.
2. Model types for WebSocket commands and data structures live in `modules/ui/src/client/commands.ts`, `modules/ui/src/client/data.ts` && `modules/ui/src/client/types.ts`.