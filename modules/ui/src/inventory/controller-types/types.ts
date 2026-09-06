import type { RootState as PeripheryState } from '../periphery-types/types'
import type { ControllerType, ControllerTypeId, InventoryState, NewEntity } from '../../types'
import type { RootState as BaseRootState } from '../../store/root-store'

export type NewControllerType = NewEntity<ControllerType>

export type ControllerTypesState = InventoryState<ControllerTypeId, ControllerType>

export type RootState = { controllerTypes: ControllerTypesState } & PeripheryState & BaseRootState
