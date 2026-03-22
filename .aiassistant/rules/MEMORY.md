---
apply: always
---

# pi-farm memory

## Code style (UI/React)

**Never use React hooks in connected components:**
- Use `connect` + `mapStateToProps` / `mapDispatchToProps` instead of `useSelector` / `useDispatch`
- Don't use `useState` / `useEffect` unless absolutely necessary
- Assume Redux state exists for most logic (add selectors/actions later if needed)

**Immutability & functional patterns:**
- Use `reduce` instead of loops when transforming arrays/objects
- Never mutate arrays or objects—always create new instances
- Avoid temporary variables and intermediate mutations

**Component structure:**
- Extract small components to keep code length down (no component > 100 lines)
- Keep JSX clean: put complex conditions into sub-components
- Use selectors to extract and filter data before passing to components
- **Micro-component pattern for conditionals:** break down even small conditionals into dedicated components
  - Example: `NoPeripheries`, `PeripheriesList`, `ShowPeripheries` (wraps conditional)
  - Each component has a single responsibility (render empty state, render list, or choose between them)
  - Makes JSX more readable and each component more testable/reusable
- **Push business logic into pure utility functions** — components focus on rendering only
  - Extract data transformations, arrays merging, computations into standalone functions (e.g., `combineNodes()`, `combineEdges()`)
  - Extract even minimal UI fragments into micro-components (e.g., `Description`, `SlotLabel`)
  - Never use inline `style={{}}` — use SCSS classes instead

## Model classes

Model classes: `modules/common/src/main/scala/org/pi/farm/model/`

### Rules when modifying model classes

1. **Always ask for clarification first** — before changing any model class, ask the user to explain intent and any domain details not obvious from the types (field format, units, invariants, etc.).

2. **Keep scaladoc up to date** — after every change, update the scaladoc on affected classes and fields. Focus on non-obvious semantics: data formats (e.g. data URLs for images), external references (e.g. URLs to soldering schema files), ordering contracts, valid value ranges.

### Key domain notes (model)

- `PeripheryType.image` — data URL (e.g. `data:image/png;base64,...`), not a file path or CDN URL
- `ControllerType.schema` — URL to a soldering/wiring schema file, not a JSON Schema
- `ControllerType.peripheries: Map[PeripheryId, PeripheryTypeId]` — maps pin/port identifiers (e.g. `"1-3"`) to the type of periphery attached there
- `Configuration.inbound` / `Configuration.outbound` — positional lists; must align with the processing unit's channel lists in order
- `ProcessingUnit.outbound` — genuinely a list; a unit can emit multiple values of different types (e.g. a Boolean flag and a Float angle)
