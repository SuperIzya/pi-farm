import type {
  PeripheryType,
  InventoryState,
  NewEntity,
  PeripheryTypeId
} from '../../types'

export type NewPeripheryType = NewEntity<PeripheryType>

export type PeripheryTypesState = InventoryState<PeripheryTypeId, PeripheryType>
export type RootState = { periphery?: PeripheryTypesState }
