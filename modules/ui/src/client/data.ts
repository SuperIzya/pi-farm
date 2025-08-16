import type { ControllerType, PeripheryType } from '../types'

export const dataTypes = ['periphery-type', 'controller-type'] as const

export type DataType = (typeof dataTypes)[number]

export type GenericData<K extends DataType, T> = {
  [k in K]: T
}

export type Data =
  | GenericData<'periphery-type', PeripheryType>
  | GenericData<'controller-type', ControllerType>
