import type {Controller, ControllerType, PeripheryType} from '../types'
import type {TransportObj} from "./types";

export const dataNames = ['peripheryType', 'controllerType', 'peripheryTypes', 'controllerTypes', 'controller', 'controllers'] as const

export type DataNames = (typeof dataNames)[number]

export type TypedData<K extends DataNames, T> = TransportObj<K, T>

export type Data =
    | TypedData<'peripheryType', PeripheryType>
    | TypedData<'controllerType', ControllerType>
    | TypedData<'controllerTypes', ControllerType[]>
    | TypedData<'peripheryTypes', PeripheryType[]>
    | TypedData<'controller', Controller>
    | TypedData<'controllers', Controller[]>

type AllData<D extends Data> = D extends Data
    ? D extends TypedData<infer A, infer B>
        ? B
        : never
    : never
type AllDataTypes = AllData<Data>

type FindDataType<T extends DataNames, D extends AllDataTypes> =
    D extends AllDataTypes
        ? TypedData<T, D> extends Data
            ? D
            : never
        : never

export type ExtractData<T extends DataNames> = FindDataType<T, AllDataTypes>
