import type { ControllerType, PeripheryType } from '../types'

export type DataType = 'periphery-type' | 'controller-type'

export type GenericData<K extends DataType, T> = {
  type: K
  data: T
}

export type Data =
  | GenericData<'periphery-type', PeripheryType>
  | GenericData<'controller-type', ControllerType>
