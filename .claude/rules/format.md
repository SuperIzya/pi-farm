# Code Formatting Rules

## Scala (Backend)

### Formatter
- **scalafmt** is enforced on compile (`scalafmtOnCompile := true`). Configuration lives in `.scalafmt.conf` if present, otherwise defaults apply.
- Run `sbt scalafmtAll` before committing if auto-format was bypassed.

### Indentation
- 2 spaces for Scala source files. No tabs.

### Naming Conventions
- **Classes/Traits/Objects**: `PascalCase` — e.g., `ConfigurationRepository`, `DbLayer`
- **Methods/Values**: `camelCase` — e.g., `getById`, `controllerType`
- **Type Parameters**: Single uppercase letter or descriptive `PascalCase` — e.g., `A`, `Env`
- **Constants**: `PascalCase` in companion objects — e.g., `val DefaultPort = 8080`
- **Packages**: all lowercase, dot-separated — `org.pi.farm.storage`
- **Files**: Match the primary class/trait/object name — `ConfigurationRepository.scala`. Have only one public class/trait/object + companion per file.
- **Test files**: `<ClassName>Spec.scala` — e.g., `ConfigurationRepositorySpec.scala`

### Import Ordering
1. Project imports (`org.pi.farm.*`)
2. Third-party libraries (`zio.*`, `doobie.*`, `io.scalaland.*`)
3. Java/Scala standard library (`java.*`, `scala.*`)
4. Group imports from the same package together
5. If more than 4 imports from the same package, use wildcard import (`import org.pi.farm.storage.*`)

### Line Length
- Maximum 120 characters. scalafmt handles wrapping.

### Scala 3 Conventions
- Don't use `derives` for typeclass derivation (ZIO JSON, ZIO Schema) if it causes issues with macro-based code generation (e.g., plugin processor builders). Instead, define givens in companion objects.
- Prefer `enum` over sealed trait hierarchies where applicable
- Don't use significant indentation (braceless syntax). Use  brace styles
- Use `extension` methods over implicit classes
- Use `given`/`using` over `implicit val`/`implicit def` for new code

### Code Structure
```scala
// Good: ZIO service pattern
trait ConfigurationRepository {
  def getAll: Task[List[FlowConfiguration]]
  def getById(id: Long): Task[Option[FlowConfiguration]]
}

object ConfigurationRepository {
  // Accessor methods
  def getAll: ZIO[ConfigurationRepository, Throwable, List[FlowConfiguration]] =
    ZIO.serviceWithZIO(_.getAll)

  // Live layer
  val live: ZLayer[Transactor[Task], Nothing, ConfigurationRepository] =
    ZLayer.fromFunction(LiveConfigurationRepository(_))
}

private class LiveConfigurationRepository(xa: Transactor[Task]) extends ConfigurationRepository {
  // implementation
}
```

## TypeScript / React (Frontend)

### Formatter
- **Prettier** via ESLint plugin. Run `npm run fix` to auto-format.

### Indentation
- 2 spaces. No tabs.

### Naming Conventions
- **Components**: `PascalCase` — e.g., `ControllerList`, `NavBar`
- **Files (components)**: `kebab-case.tsx` — e.g., `controller-list.tsx`, `nav-bar.tsx`
- **Files (utilities)**: `kebab-case.ts` — e.g., `root-store.ts`, `routes.ts`
- **Variables/Functions**: `camelCase` — e.g., `handleClick`, `controllerTypes`
- **Constants**: `SCREAMING_SNAKE_CASE` for true constants, `camelCase` for derived values
- **Types/Interfaces**: `PascalCase` — e.g., `ControllerType`, `AppState`
- **Redux slices**: `camelCase` name, file named after feature — e.g., `controller-types/store.ts`
- **SCSS modules**: `kebab-case.scss`

### Import Ordering
1. React and React-related (`react`, `react-dom`, `react-redux`, `react-router-dom`)
2. Third-party libraries (`@reduxjs/toolkit`, `@mui/*`, `@xyflow/*`)
3. Project aliases / absolute paths
4. Relative imports (parent `../` before sibling `./`)
5. Style imports last

### Line Length
- Maximum 100 characters for code, Prettier handles wrapping.

### TypeScript Conventions
- Strict mode enabled (`tsconfig.json`)
- Prefer `type` for object shapes, unions/intersections
- No `any` — use `unknown` and narrow with type guards
- Use `as const` for literal types where appropriate
- Use `reduce` instead of loops when transforming arrays/objects
- Never mutate arrays or objects—always create new instances
- Avoid temporary variables and intermediate mutations

### React Conventions
- Functional components only — no class components
- **`connect()` over hooks** — use Redux `connect()` with selectors for data access, not `useSelector`/`useDispatch`. Reserve hooks for non-Redux concerns (e.g., `useSendCommand()` for WebSocket)
- Redux Toolkit `createSlice` for all state management
- Co-locate actions, selectors, slices, and store in the feature directory
- **`itemKey`-based data lookup** — list items receive an index (`itemKey`) rather than entity data as props. Each micro-component uses `connect()` to look up its own slice from the store via that index and `getListKey`
- **Granular connected micro-components** — each piece of display data gets its own connected component (e.g., `Name`, `TypeName`, `Description`, `EditBtn`, `DeleteBtn`). They individually select their own data from the store
- **Selector factories** — use higher-order functions that create selectors from an extractor (e.g., `controllerSelector(({ name }) => ({ text: name }))`). Use factory functions returning new selector instances (e.g., `() => controllerSelector(...)`) to avoid cache collisions across multiple component instances
- **Connector reuse** — define a connector once (e.g., `connectId = connect(controllerIdSelector)`) and reuse it for multiple components that need the same data
- **Shared base component with specialization** — create a thin wrapper component (e.g., `TextComponent`) and produce multiple connected variants by connecting it with different selectors
- **Composition over conditionals** — wrap loading state with `WaitLoading`, compose `GenericList` > `Item` > micro-components. Avoid `if/else` in render bodies; delegate to wrapper components
- **Component structure:**
  - Extract small components to keep code length down (no component > 100 lines)
  - Keep JSX clean: put complex conditions into sub-components
  - Extract even minimal UI fragments into micro-components (e.g., `Description`, `SlotLabel`)
- **Push business logic into pure utility functions** — components focus on rendering only
  - Extract data transformations, arrays merging, computations into standalone functions (e.g., `combineNodes()`, `combineEdges()`)
  - Never use inline `style={{}}` (except for setting up CSS vars) — use SCSS classes.


### Example Component Structure
```tsx
import React from 'react'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'

import { useSendCommand } from '../../client'
import { Controller, IdType } from '../../types'
import { GenericList, GenericListProps, getListKey, ListItem } from '../../utils/list-mixin'
import { WaitLoading } from '../../utils/wait-loading'
import { Text } from '../../utils/text'
import { DeleteButton, EditButton, AddButton } from '../form-mixin'
import { getIsLoading, getKnownEntities } from './selectors'
import * as styles from './list.scss'

// Selector factory: creates a selector from an extractor function
const controllerSelector = <T,>(f: (c: Controller) => T) =>
  createSelector([getKnownEntities, getListKey], (controllers, itemKey) =>
    f(controllers[itemKey])
  )

// Thin base component — specialized via connect()
const TextComponent = ({ text, className }: { text: string, className?: string }) => (
  <Text className={className} text={text} />
)

// Granular connected micro-components — each selects its own data
const Name = connect(() => controllerSelector(({ name: text }) => ({ text })))(
  TextComponent
)
const Description = connect(() =>
  controllerSelector(({ description: text }) => ({ text }))
)(TextComponent)

// Connector reuse — define once, apply to multiple components
const controllerIdSelector = () => controllerSelector(({ id }) => ({ id }))
const connectId = connect(controllerIdSelector)

const EditBtn = connectId(({ id }: { id: IdType }) => (
  <EditButton className={styles.editButton} id={id} />
))
const DeleteBtn = connectId(
  ({ id, sendDelete }: { id: IdType, sendDelete: (id: IdType) => void }) => (
    <DeleteButton id={id} className={styles.deleteButton} onDelete={sendDelete} />
  )
)

// Item component — receives itemKey, micro-components look up their own data
const Item: ListItem<{ sendDelete: (id: IdType) => void }> = ({ itemKey, sendDelete }) => (
  <div className={styles.item}>
    <Name itemKey={itemKey} className={styles.name} />
    <Description itemKey={itemKey} className={styles.description} />
    <EditBtn itemKey={itemKey} />
    <DeleteBtn sendDelete={sendDelete} itemKey={itemKey} />
  </div>
)

// List wired to store count — GenericList handles iteration
const mapCount = createSelector([getKnownEntities], ({ length }) => ({ count: length }))
const List = connect(mapCount)((p: GenericListProps<{ sendDelete: (id: IdType) => void }>) => (
  <GenericList {...p} />
))

// Top-level: only place hooks are used (WebSocket command sender)
export const ControllerList = () => {
  const send = useSendCommand()
  const sendDelete = (id: IdType) => send('delete-controller', id)
  return (
    <div className={styles.container}>
      <h1>Controllers</h1>
      <AddButton className={styles.add} text={'Add new controller'} />
      <WaitLoading isLoadingSelector={getIsLoading}>
        <List containerClassName={styles.list} sendDelete={sendDelete} Item={Item} />
      </WaitLoading>
    </div>
  )
}
```

## SCSS
- Use variables from `utils/variables.scss` — never hardcode colors or spacing
- Use mixins from `utils/mixins.scss` for reusable patterns
- BEM naming convention for class names: `block__element--modifier`
