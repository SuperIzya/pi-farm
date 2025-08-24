import type { RootState as PeripheryRootState } from '../periphery-types/types'
import type { ControllerType, InventoryState, NewEntity } from '../../../types'

export type NewControllerType = NewEntity<ControllerType>

export type ControllerTypesState = InventoryState<ControllerType>

export type RootState = { controllerTypes: ControllerTypesState } & PeripheryRootState
