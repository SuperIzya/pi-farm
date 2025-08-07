import type { RootState as PeripheryRootState } from '../periphery-types/types'
import type { ControllerType } from '../../../types'
import type { InventoryState, NewType } from '../types'

export type NewControllerType = NewType<ControllerType>

export type ControllerTypesState = InventoryState<ControllerType>

export type RootState = { controllerTypes: ControllerTypesState } & PeripheryRootState
