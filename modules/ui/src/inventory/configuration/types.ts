import type {
  Configuration,
  InventoryState,
  NewEntity,
  ProcessingUnit
} from '../../types'
import type { RootState as ControllerState } from '../controller/types'

export type NewConfiguration = NewEntity<Configuration>
export type ConfigurationsState = InventoryState<Configuration> & {
  isInitialized: boolean
}

export type ProcessingUnitsState = {
  isInitialized: boolean
  isLoading: boolean
  entities: ProcessingUnit[]
}

export type RootState = {
  configurations: ConfigurationsState
  processingUnits: ProcessingUnitsState
} & ControllerState
