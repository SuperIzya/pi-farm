import type { ControllerType, PeripheryType } from '../types'

export type CommandName =
  | 'save-periphery-type'
  | 'update-periphery-type'
  | 'save-controller-type'
  | 'update-controller-type'
  | 'get-periphery-types'
  | 'get-controller-types'

type Data<T> = T extends void ? Record<string, never> : { data: T }

export type CommandObj<T extends CommandName, D = void> = {
  [k in T]: Data<D>
}

export type NoId<T> = Omit<T, 'id'>

export type Command =
  | CommandObj<'save-periphery-type', NoId<PeripheryType>>
  | CommandObj<'save-controller-type', NoId<ControllerType>>
  | CommandObj<'update-periphery-type', PeripheryType>
  | CommandObj<'update-controller-type', ControllerType>
  | CommandObj<'get-periphery-types'>
  | CommandObj<'get-controller-types'>

export type ProperName<T extends CommandName, D> =
  CommandObj<T, D> extends Command ? T : never

type FieldTypes<T, K extends keyof T = keyof T> = K extends keyof T ? T[K] : never
type IsCommandVoid<T extends CommandName> = CommandObj<T> extends Command ? true : false
type AllButField<T extends CommandName, D, K extends keyof D> = undefined extends D[K]
  ? number
  : CommandObj<T, Omit<D, K>> extends Command
    ? void
    : D[K]

type AllBut<T extends CommandName, D> = {
  [k in keyof D]: AllButField<T, D, k>
}

type NoVoids<T extends CommandName, D> =
  CommandObj<T, D> extends Command
    ? void extends FieldTypes<AllBut<T, D>>
      ? never
      : D
    : never

export type ProperData<T extends CommandName, D> =
  IsCommandVoid<T> extends true ? (D extends void ? D : never) : NoVoids<T, D>
