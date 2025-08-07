import type { PeripheryType } from '../../../types'
import type { InventoryState, NewType } from '../types'

export type NewPeripheryType = NewType<PeripheryType>

export type PeripheryTypesState = InventoryState<PeripheryType>
export type RootState = { periphery?: PeripheryTypesState }
