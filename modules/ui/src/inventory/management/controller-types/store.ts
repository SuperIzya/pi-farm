import type { ControllerTypesState, NewControllerType } from './types'
import { createSlice, PayloadAction, type WithSlice } from '@reduxjs/toolkit'
import { rootReducer } from '../../../store/root-store'
import type { ControllerType, Peripheries } from '../../../types'
import { defaultInventoryActions, defaultInventorySelectors } from '../store-mixin'

const initialState: ControllerTypesState = {
  knownTypes: [],
  isLoading: true
}

const emptyNewType: NewControllerType = { canBeSaved: false }

const controllerStore = createSlice({
  name: 'controllerTypes',
  initialState,
  reducers: {
    ...defaultInventoryActions<ControllerType, ControllerTypesState>(emptyNewType),
    setNewTypeName: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        name: action.payload
      }
    }),
    setNewTypeDescription: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        description: action.payload
      }
    }),
    setNewTypeSchema: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        schema: action.payload
      }
    }),
    setNewTypeCode: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        code: action.payload
      }
    }),
    removeNewTypePeriphery: (state, action: PayloadAction<string>) => {
      if (!state.newType?.peripheries) return state
      if (!(action.payload in state.newType.peripheries)) return state

      const { [action.payload]: _, ...peripheries } = state.newType.peripheries
      return {
        ...state,
        newType: {
          ...(state.newType || emptyNewType),
          peripheries
        }
      }
    },
    addNewTypePeriphery: (state, action: PayloadAction<Peripheries>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        peripheries: {
          ...(state.newType?.peripheries || {}),
          ...action.payload
        }
      }
    }),
    setNewTypeCanBeSaved: (state, action: PayloadAction<boolean>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        canBeSaved: action.payload
      }
    })
  },
  selectors: defaultInventorySelectors<ControllerType, ControllerTypesState>()
})

declare module '../../../store/root-store' {
  export type LazySlice = {} & WithSlice<typeof controllerTypesSlice>
}

export const controllerTypesSlice = controllerStore.injectInto(rootReducer)
