import type { ControllerTypesState, NewControllerType } from './types'
import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { rootReducer } from '../../store/root-store'
import type { ControllerType, ControllerTypeId, Peripheries } from '../../types'
import {
  defaultInventoryActions,
  defaultInventorySelectors,
  NewEntityPayload
} from '../store-mixin'

const initialState: ControllerTypesState = {
  knownEntities: [],
  isLoading: true,
  isInitialized: false,
  isUploading: false
}

const emptyNewEntity: NewControllerType = { canBeSaved: false }
const inventoryActions = defaultInventoryActions<
  ControllerTypeId,
  ControllerType,
  ControllerTypesState
>(emptyNewEntity)

const controllerStore = createSlice({
  name: 'controllerTypes',
  initialState,
  reducers: {
    ...inventoryActions,
    editEntity: (state, action: PayloadAction<ControllerTypeId>) =>
      state.newEntity?.id === action.payload
        ? state
        : inventoryActions.editEntity({ ...state, newEntity: undefined }, action),
    addNewEntity: (state, action: PayloadAction<ControllerType>) => ({
      ...inventoryActions.addNewEntity(state, action),
      newEntity: undefined
    }),
    setUploading: (state, action: PayloadAction<boolean>) => ({
      ...state,
      isUploading: action.payload
    }),
    setNewEntityName: (state, action: NewEntityPayload<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        name: action.payload
      }
    }),
    setNewEntityDescription: (state, action: NewEntityPayload<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        description: action.payload
      }
    }),
    setNewEntitySchema: (state, action: NewEntityPayload<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        schema: action.payload
      }
    }),
    setNewEntityCodeArchive: (state, action: NewEntityPayload<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        codeArchive: action.payload
      }
    }),
    removeNewEntityPeriphery: (state, action: PayloadAction<string>) => {
      if (!state.newEntity?.peripheries) return state
      if (!(action.payload in state.newEntity.peripheries)) return state

      const { [action.payload]: _, ...peripheries } = state.newEntity.peripheries
      return {
        ...state,
        newEntity: {
          ...(state.newEntity || emptyNewEntity),
          peripheries
        }
      }
    },
    addNewEntityPeriphery: (state, action: PayloadAction<Peripheries>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        peripheries: {
          ...(state.newEntity?.peripheries || {}),
          ...action.payload
        }
      }
    })
  },
  selectors: {
    ...defaultInventorySelectors<ControllerTypeId, ControllerType, ControllerTypesState>(
      emptyNewEntity
    ),
    getNewEntity: ({ newEntity }: ControllerTypesState) => newEntity,
    getIsUploading: ({ isUploading }: ControllerTypesState) => isUploading
  }
})

export const controllerTypesSlice = controllerStore.injectInto(rootReducer)
