import type {
  Controller,
  ControllerId,
  InventoryState,
  NewEntity,
  ProcessLog,
  ProcessState
} from '../../types'
import type { RootState as ControllerTypesState } from '../controller-types/types'

export type NewController = NewEntity<Controller>
export type ControllersState = InventoryState<ControllerId, Controller>
export type ProcessConsoleState = {
  isOpen: boolean
  logs: ProcessLog[]
  process?: ProcessState
}
export type RootState = {
  controllers: ControllersState
  processConsole: ProcessConsoleState
} & ControllerTypesState
