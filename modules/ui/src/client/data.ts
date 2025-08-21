import type { Controller, ControllerType, PeripheryType } from '../types'
import type { TransportObj } from './types'

export const dataNames = [
  'periphery-type',
  'controller-type',
  'periphery-types',
  'controller-types',
  'controller',
  'controllers'
] as const

export type DataNames = (typeof dataNames)[number]

export type TypedData<K extends DataNames, T> = TransportObj<K, T>

export type Data =
  | TypedData<'periphery-type', PeripheryType>
  | TypedData<'controller-type', ControllerType>
  | TypedData<'controller-types', ControllerType[]>
  | TypedData<'periphery-types', PeripheryType[]>
  | TypedData<'controller', Controller>
  | TypedData<'controllers', Controller[]>

type AllData<D extends Data> = D extends Data
  ? D extends TypedData<infer _A, infer B>
    ? B
    : never
  : never
type AllDataTypes = AllData<Data>

type FindDataType<T extends DataNames, D extends AllDataTypes> = D extends AllDataTypes
  ? TypedData<T, D> extends Data
    ? D
    : never
  : never

export type ExtractData<T extends DataNames> = FindDataType<T, AllDataTypes>

const isDataTyped = <T extends DataNames>(
  name: T,
  obj: object
): obj is TypedData<T, ExtractData<T>> => name in obj

export const findDataType =
  <T extends DataNames>(obj: object) =>
  (name: T): TypedData<T, ExtractData<T>> | undefined => {
    if (isDataTyped(name, obj)) {
      return obj
    }
  }
