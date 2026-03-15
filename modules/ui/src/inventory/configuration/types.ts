import type { Configuration, InventoryState, NewEntity } from '../../types'
import type { RootState as ControllerState } from '../controller/types'

export type NewConfiguration = NewEntity<Configuration>
export type ConfigurationsState = InventoryState<Configuration> & {
  isInitialized: boolean
}

export type RootState = { configurations: ConfigurationsState } & ControllerState
