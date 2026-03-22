import type {
  Configuration,
  ControllerId,
  InventoryState,
  NewEntity,
  ProcessingUnit
} from '../../types'
import type { RootState as ControllerState } from '../controller/types'

export type CurrentGraph = {
  selectedControllerId?: ControllerId
  selectedPeripheryId?: string
}

export type NewConfiguration = NewEntity<Configuration>
export type ConfigurationsState = {
  isInitialized: boolean
} & InventoryState<ControllerId, Configuration>

export type ProcessingUnitsState = {
  isInitialized: boolean
  isLoading: boolean
  entities: {
    [name: string]: ProcessingUnit
  }
}

export type RootState = {
  configurations: ConfigurationsState
  processingUnits: ProcessingUnitsState
  currentGraph: CurrentGraph
} & ControllerState
