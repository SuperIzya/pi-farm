import type { PayloadAction } from '@reduxjs/toolkit'
import { createSlice } from '@reduxjs/toolkit'
import { PeripheryTypesState, NewPeripheryType } from './types'
import { rootReducer } from '../../../store/root-store'
import type { PeripheryDirection, PeripheryType } from '../../../types'
import { defaultInventoryActions, defaultInventorySelectors } from '../store-mixin'

const initialState: PeripheryTypesState = {
  knownTypes: [],
  isLoading: true
}

const emptyNewType: NewPeripheryType = {
  canBeSaved: false
}
const slice = createSlice({
  name: 'periphery',
  initialState,
  reducers: {
    ...defaultInventoryActions(emptyNewType),
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
    setNewTypeImage: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        image: action.payload
      }
    }),
    setNewTypeDirection: (
      state,
      action: PayloadAction<PeripheryDirection | undefined>
    ) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        direction: action.payload
      }
    }),
    setNewTypeUnits: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newType: {
        ...(state.newType || emptyNewType),
        units: action.payload
      }
    })
  },
  selectors: defaultInventorySelectors<PeripheryType, PeripheryTypesState>()
})

export const peripheryTypesSlice = slice.injectInto(rootReducer)
