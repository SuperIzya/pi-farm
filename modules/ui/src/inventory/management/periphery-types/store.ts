import type { PayloadAction, WithSlice } from '@reduxjs/toolkit'
import { createSlice } from '@reduxjs/toolkit'
import {
  PeripheryDirection,
  PeripheryTypesState,
  PeripheryType,
  NewPeripheryType
} from './types'
import { rootReducer } from '../../../store/root-store'

const initialState: PeripheryTypesState = {
  knownTypes: []
}

const emptyNewType: NewPeripheryType = {
  canBeSaved: false
}

const slice = createSlice({
  name: 'periphery',
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
    setTypes: (state, action: PayloadAction<PeripheryType[]>) => ({
      knownTypes: action.payload
    }),
    setNewTypeCanBeSaved: (state, action: PayloadAction<boolean>) => ({
      ...state,
      newType: {
        ...(state.newType || {}),
        canBeSaved: action.payload
      }
    }),
    startNewType: (state) => ({
      ...state,
      newType: emptyNewType
    }),
    cancelNewType: (state) => ({
      ...state,
      newType: undefined
    }),
    setNewTypeName: (state, action: PayloadAction<string>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        name: action.payload
      }
    }),
    setNewTypeDescription: (state, action: PayloadAction<string>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        description: action.payload
      }
    }),
    setNewTypePicture: (state, action: PayloadAction<string | null>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        picture: action.payload
      }
    }),
    setNewTypeDirection: (state, action: PayloadAction<PeripheryDirection>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        direction: action.payload
      }
    }),
    setNewTypeUnits: (state, action: PayloadAction<string>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        units: action.payload
      }
    }),
    saveNewType: (state) => state,
    addNewType: (state, action: PayloadAction<PeripheryType>) => {
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
  export interface LazySlice extends WithSlice<typeof peripheryTypesSlice> {}
}

export const peripheryTypesSlice = slice.injectInto(rootReducer)
