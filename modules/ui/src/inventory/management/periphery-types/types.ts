import type { PeripheryType, InventoryState, NewEntity } from '../../../types'

export type NewPeripheryType = NewEntity<PeripheryType>

export type PeripheryTypesState = InventoryState<PeripheryType>
export type RootState = { periphery?: PeripheryTypesState }
