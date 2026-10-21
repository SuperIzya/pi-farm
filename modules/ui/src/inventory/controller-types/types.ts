import type { RootState as PeripheryState } from '../periphery-types/types'
import type { ControllerType, ControllerTypeId, InventoryState, NewEntity } from '../../types'

export type ControllerTypeDraft = ControllerType & { codeArchive?: string }

export type NewControllerType = NewEntity<ControllerTypeDraft>

export type ControllerTypesState = InventoryState<
  ControllerTypeId,
  ControllerType,
  ControllerTypeDraft
> & { isUploading: boolean }

export type RootState = { controllerTypes: ControllerTypesState } & PeripheryState
