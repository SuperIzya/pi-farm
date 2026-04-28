import type { RootState as PeripheryRootState } from '../periphery-types/types'
import type { ControllerType, ControllerTypeId, InventoryState, NewEntity } from '../../types'

export type NewControllerType = NewEntity<ControllerType>

export type ControllerTypesState = InventoryState<ControllerTypeId, ControllerType>

export type RootState = { controllerTypes: ControllerTypesState } & PeripheryRootState
