import type { InventoryState, NewType } from '../types'
import type { RootState as PeripheryRootState } from '../periphery-types/types'

export type ControllerType = {
  id: number
  name: string
  description: string
  schema?: string
  code: string
  periphery: number[]
}

export type NewControllerType = NewType<ControllerType>

export type ControllerTypesState = InventoryState<ControllerType>
export type RootState = { controllerTypes: ControllerTypesState } & PeripheryRootState
