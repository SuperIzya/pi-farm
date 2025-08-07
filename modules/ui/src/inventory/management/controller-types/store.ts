import type { ControllerTypesState, NewControllerType } from './types'
import { createSlice, PayloadAction, type WithSlice } from '@reduxjs/toolkit'
import { rootReducer } from '../../../store/root-store'
import type { ControllerType, Peripheries } from '../../../types'

const initialState: ControllerTypesState = {
  knownTypes: []
}

const emptyNewType: NewControllerType = { canBeSaved: false }

const controllerStore = createSlice({
  name: 'controllerTypes',
  initialState,
  reducers: {
    editType: (state, action: PayloadAction<number>) => {
      const index = state.knownTypes.findIndex((t) => t.id === action.payload)
      if (index < 0) return state
      const edit = state.knownTypes[index]
      return {
        ...state,
        newType: {
          ...(edit || {}),
          canBeSaved: false
        },
        editingIndex: index
      }
    },
    setTypes: (state, action: PayloadAction<ControllerType[]>) => ({
      knownTypes: action.payload
    }),
    startNewType: (state) => ({
      ...state,
      newType: emptyNewType
    }),
    cancelNewType: (state) => ({
      ...state,
      newType: undefined
    }),
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
    }),
    saveNewType: (state) => state,
    addNewType: (state, action: PayloadAction<ControllerType>) => {
      const index = state.editingIndex || state.knownTypes.length
      const before = state.knownTypes.slice(0, index)
      const after = state.knownTypes.slice(index)
      return {
        knownTypes: [...before, action.payload, ...after],
        newType: undefined,
        editingIndex: undefined
      }
    }
  },
  selectors: {
    getKnownTypes: ({ knownTypes }) => knownTypes,
    getNewType: ({ newType }) => newType
  }
})

declare module '../../../store/root-store' {
  export interface LazySlice extends WithSlice<typeof controllerTypesSlice> {}
}

export const controllerTypesSlice = controllerStore.injectInto(rootReducer)
