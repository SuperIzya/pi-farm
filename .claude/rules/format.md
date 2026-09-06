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
- **Typed `useSelector` over `connect()`** — create a module-local typed hook with
  `const useCSelector = useSelector.withTypes<RootState>()` and read store data through it.
  `connect()` is no longer the default; keep it only in legacy code not yet migrated
- Redux Toolkit `createSlice` for all state management
- Co-locate actions, selectors, slices, types, and store in the feature directory
- **`selector`-prop data lookup** — list items build a memoized entity selector once
  (`createSelector(getKnownEntities, (e) => e[itemKey])`) and pass it down as a `selector` prop.
  Micro-components receive `SelectorProps<Entity, RootState>` and resolve their own field from it,
  instead of receiving entity data as props
- **Granular micro-components** — each piece of display data gets its own component
  (e.g., `Name`, `Description`, `SvgPreview`, `EditBtn`, `DeleteBtn`) that selects only what it renders
- **Shared base component with specialization** — create a thin presentational component
  (e.g., `TextComponent`) and specialize it inside each micro-component
- **Composition over conditionals** — wrap loading state with `WaitLoading`, compose
  `GenericList` > `Item` > micro-components. Avoid `if/else` in render bodies
- **Hooks usage** — `useSendCommand()` for WebSocket commands, `useCSelector` for store reads.
  Prefer deriving values (e.g., `count`) from selectors over storing them in props
- **Component structure:**
  - Extract small components to keep code length down (no component > 100 lines)
  - Keep JSX clean: put complex conditions into sub-components
  - Extract even minimal UI fragments into micro-components (e.g., `Description`, `SlotLabel`)
- **Push business logic into pure utility functions** — components focus on rendering only
  - Extract data transformations, arrays merging, computations into standalone functions
  - Never use inline `style={{}}` (except for setting up CSS vars) — use SCSS classes


### Example Component Structure
```tsx
import React from 'react'
import { useSelector } from 'react-redux'
import { createSelector } from 'reselect'

import { useSendCommand } from '../../client'
import type { Controller, IdType, SelectorProps } from '../../types'
import { GenericList, GenericListProps, ListItem } from '../../utils/list-mixin'
import { WaitLoading } from '../../utils/wait-loading'
import { Text } from '../../utils/text'
import { ClassName, DeleteButton, EditButton, AddButton } from '../form-mixin'
import { getIsLoading, getKnownEntities } from './selectors'
import { RootState } from './types'
import * as styles from './list.scss'

type CtrlSelProps = SelectorProps<Controller, RootState>
const useCSelector = useSelector.withTypes<RootState>()

// Thin presentational base component
const TextComponent = ({ text, className }: { text: string } & ClassName) => (
  <Text className={className} text={text} />
)

// Micro-components resolve their own field from the passed selector
const Name = ({ className, selector }: ClassName & CtrlSelProps) => {
  const { name } = useCSelector(selector)
  return <TextComponent text={name} className={className} />
}

const Description = ({ className, selector }: ClassName & CtrlSelProps) => {
  const { description } = useCSelector(selector)
  return <TextComponent text={description} className={className} />
}

type ItemProps = { sendDelete: (id: IdType) => void }

const EditBtn = ({ selector }: CtrlSelProps) => {
  const { id } = useCSelector(selector)
  return <EditButton className={styles.editButton} id={id} />
}

const DeleteBtn = ({ selector, sendDelete }: CtrlSelProps & ItemProps) => {
  const { id } = useCSelector(selector)
  return <DeleteButton id={id} className={styles.deleteButton} onDelete={sendDelete} />
}

// Item builds the entity selector once from itemKey and passes it down
const Item: ListItem<ItemProps> = ({ itemKey, sendDelete }) => {
  const selector = createSelector(getKnownEntities, (entities) => entities[itemKey])
  return (
    <div className={styles.item}>
      <Name selector={selector} className={styles.name} />
      <Description selector={selector} className={styles.description} />
      <EditBtn selector={selector} />
      <DeleteBtn selector={selector} sendDelete={sendDelete} />
    </div>
  )
}

const List = (p: Omit<GenericListProps<ItemProps>, 'count'>) => {
  const count = useCSelector(getKnownEntities).length
  return <GenericList {...p} count={count} />
}

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
