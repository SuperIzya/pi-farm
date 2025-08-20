import type {Controller, ControllerType, PeripheryType} from '../types'
import type {TransportObj} from "./types";

export type CommandName =
    | 'savePeripheryType'
    | 'updatePeripheryType'
    | 'saveControllerType'
    | 'updateControllerType'
    | 'getPeripheryTypes'
    | 'getControllerTypes'
    | 'getControllers'
    | 'saveController'
    | 'updateController'

export type CommandObj<T extends CommandName, D = void> = TransportObj<T, D>

export type NoId<T> = Omit<T, 'id'>

export type Command =
    | CommandObj<'savePeripheryType', NoId<PeripheryType>>
    | CommandObj<'saveControllerType', NoId<ControllerType>>
    | CommandObj<'updatePeripheryType', PeripheryType>
    | CommandObj<'updateControllerType', ControllerType>
    | CommandObj<'getControllers'>
    | CommandObj<'saveController', NoId<Controller>>
    | CommandObj<'updateController', Controller>
    | CommandObj<'getPeripheryTypes'>
    | CommandObj<'getControllerTypes'>

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
