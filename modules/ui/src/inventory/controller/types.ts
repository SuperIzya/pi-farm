import type { Controller, InventoryState, NewEntity } from '../../types'
import type { RootState as PeripheryTypesState } from '../periphery-types/types'
import type { RootState as ControllerTypesState } from '../controller-types/types'

export type NewController = NewEntity<Controller>
export type ControllersState = InventoryState<Controller>
export type RootState = { controllers: ControllersState } & PeripheryTypesState &
  ControllerTypesState
