import type {
  PeripheryType,
  InventoryState,
  NewEntity,
  PeripheryTypeId,
  PeripheryConnection
} from '../../types'

export type NewPeripheryType = NewEntity<PeripheryType>

export type PeripheryTypesState = InventoryState<PeripheryTypeId, PeripheryType> & {
  newConnection?: Partial<PeripheryConnection> & { canBeSaved: boolean }
}
export type PeripheryConnectionsState = {
  current: PeripheryConnection
}
export type RootState = { periphery?: PeripheryTypesState, peripheryConnections?: PeripheryConnectionsState }
