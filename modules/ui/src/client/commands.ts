import type {
  PeripheryTypeId,
  ControllerTypeId,
  ControllerId,
  ConfigurationId,
  New,
  Controller,
  ControllerType,
  PeripheryType,
  Configuration
} from '../types'
import type { PartialMessage, TransportObj } from './types'

export type CommandObj<T extends CommandName, D = void> = TransportObj<T, D>

export type CommandName =
  | 'save-configuration'
  | 'update-configuration'
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
  | 'get-processing-units'
  | 'delete-periphery-type'
  | 'delete-controller-type'
  | 'delete-controller'
  | 'delete-configuration'
  | 'partial-command'

export type Command =
  | CommandObj<'save-configuration', New<Configuration>>
  | CommandObj<'update-configuration', Configuration>
  | CommandObj<'delete-periphery-type', PeripheryTypeId>
  | CommandObj<'delete-controller-type', ControllerTypeId>
  | CommandObj<'delete-controller', ControllerId>
  | CommandObj<'delete-configuration', ConfigurationId>
  | CommandObj<'save-periphery-type', New<PeripheryType>>
  | CommandObj<'save-controller-type', New<ControllerType>>
  | CommandObj<'update-periphery-type', PeripheryType>
  | CommandObj<'update-controller-type', ControllerType>
  | CommandObj<'save-controller', New<Controller>>
  | CommandObj<'update-controller', Controller>
  | CommandObj<'get-periphery-types'>
  | CommandObj<'get-controller-types'>
  | CommandObj<'get-controllers'>
  | CommandObj<'get-configurations'>
  | CommandObj<'get-processing-units'>
  | CommandObj<'partial-command', PartialMessage>

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
