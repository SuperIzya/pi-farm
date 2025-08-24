import type { ControllerTypesState, NewControllerType } from './types'
import { createSlice, PayloadAction, type WithSlice } from '@reduxjs/toolkit'
import { rootReducer } from '../../../store/root-store'
import type { ControllerType, Peripheries } from '../../../types'
import { defaultInventoryActions, defaultInventorySelectors } from '../../store-mixin'

const initialState: ControllerTypesState = {
  knownEntities: [],
  isLoading: true
}

const emptyNewEntity: NewControllerType = { canBeSaved: false }

const controllerStore = createSlice({
  name: 'controllerTypes',
  initialState,
  reducers: {
    ...defaultInventoryActions<ControllerType, ControllerTypesState>(emptyNewEntity),
    setNewEntityName: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        name: action.payload
      }
    }),
    setNewEntityDescription: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        description: action.payload
      }
    }),
    setNewEntitySchema: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        schema: action.payload
      }
    }),
    setNewEntityCode: (state, action: PayloadAction<string | undefined>) => ({
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
  selectors: defaultInventorySelectors<ControllerType, ControllerTypesState>()
})

export const controllerTypesSlice = controllerStore.injectInto(rootReducer)
