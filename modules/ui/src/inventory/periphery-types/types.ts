import type {
  PeripheryType,
  InventoryState,
  NewEntity,
  PeripheryTypeId,
  PeripheryConnection
} from '../../types'

export type NewPeripheryType = NewEntity<PeripheryType>

export type NewConnection = NewEntity<PeripheryConnection>

export type PeripheryTypesState = InventoryState<PeripheryTypeId, PeripheryType> & {
  newConnection?: NewConnection
}
export type PeripheryConnectionsState = {
  current: PeripheryConnection
}
export type RootState = {
  periphery?: PeripheryTypesState
}
