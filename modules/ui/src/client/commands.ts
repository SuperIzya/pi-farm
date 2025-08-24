import type { IdType, NoId, Controller, ControllerType, PeripheryType } from '../types'
import type { TransportObj } from './types'

export type CommandObj<T extends CommandName, D = void> = TransportObj<T, D>

export type CommandName =
  | 'save-periphery-type'
  | 'update-periphery-type'
  | 'save-controller-type'
  | 'update-controller-type'
  | 'get-periphery-types'
  | 'get-controller-types'
  | 'get-controllers'
  | 'save-controller'
  | 'update-controller'
  | 'get-configurations'
  | 'delete-periphery-type'
  | 'delete-controller-type'
  | 'delete-controller'
  | 'delete-configuration'

export type Command =
  | CommandObj<'delete-periphery-type', IdType>
  | CommandObj<'delete-controller-type', IdType>
  | CommandObj<'delete-controller', IdType>
  | CommandObj<'delete-configuration', IdType>
  | CommandObj<'save-periphery-type', NoId<PeripheryType>>
  | CommandObj<'save-controller-type', NoId<ControllerType>>
  | CommandObj<'update-periphery-type', PeripheryType>
  | CommandObj<'update-controller-type', ControllerType>
  | CommandObj<'save-controller', NoId<Controller>>
  | CommandObj<'update-controller', Controller>
  | CommandObj<'get-periphery-types'>
  | CommandObj<'get-controller-types'>
  | CommandObj<'get-controllers'>
  | CommandObj<'get-configurations'>

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

type Test<T extends CommandName, A, B> = IsCommandVoid<T> extends true ? A : B
const a: Test<'delete-controller-type', true, false> = false
console.log(a)
