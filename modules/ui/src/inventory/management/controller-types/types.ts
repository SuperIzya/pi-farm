import type { InventoryState, NewType } from '../types'
import type { RootState as PeripheryRootState } from '../periphery-types/types'

export type Peripheries = { [key: string]: number }

export type ControllerType = {
  id: number
  name: string
  description: string
  schema?: string
  code: string
  peripheries: Peripheries
}

export type NewControllerType = NewType<ControllerType>

export type ControllerTypesState = InventoryState<ControllerType>
export type RootState = { controllerTypes: ControllerTypesState } & PeripheryRootState
