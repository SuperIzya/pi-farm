import type { ControllerTypesState, NewControllerType } from './types'
import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { rootReducer } from '../../store/root-store'
import type { Peripheries } from '../../types'
import {
  defaultInventoryActions,
  defaultInventorySelectors,
  NewEntityPayload
} from '../store-mixin'

const initialState: ControllerTypesState = {
  knownEntities: [],
  isLoading: true,
  isInitialized: false
}

const emptyNewEntity: NewControllerType = { canBeSaved: false }

const controllerStore = createSlice({
  name: 'controllerTypes',
  initialState,
  reducers: {
    ...defaultInventoryActions(emptyNewEntity),
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
    setNewEntityCode: (state, action: NewEntityPayload<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        code: action.payload
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
  selectors: defaultInventorySelectors(emptyNewEntity)
})

export const controllerTypesSlice = controllerStore.injectInto(rootReducer)
