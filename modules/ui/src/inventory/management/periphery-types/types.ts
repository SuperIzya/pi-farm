import type { InventoryState, NewType } from '../types'

export type PeripheryDirection = 'in' | 'out' | 'both'

export type PeripheryType = {
  id: number
  name: string
  description: string
  picture: string | null
  direction: PeripheryDirection
  units: string
}

export type NewPeripheryType = NewType<PeripheryType>

export type PeripheryTypesState = InventoryState<PeripheryType>

export type RootState = { periphery?: PeripheryTypesState }
