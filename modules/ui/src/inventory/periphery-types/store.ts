import type { PayloadAction } from '@reduxjs/toolkit'
import { createSlice } from '@reduxjs/toolkit'
import { PeripheryTypesState, NewPeripheryType } from './types'
import { rootReducer } from '../../store/root-store'
import type { PeripheryDirection, PeripheryType } from '../../types'
import { defaultInventoryActions, defaultInventorySelectors } from '../store-mixin'

const initialState: PeripheryTypesState = {
  knownEntities: [],
  isLoading: true
}

const emptyNewEntity: NewPeripheryType = {
  canBeSaved: false
}
const slice = createSlice({
  name: 'periphery',
  initialState,
  reducers: {
    ...defaultInventoryActions(emptyNewEntity),
    cancelNewEntity: (state) => ({
      ...state,
      newEntity: undefined
    }),
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
    setNewEntityImage: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        image: action.payload
      }
    }),
    setNewEntityDirection: (
      state,
      action: PayloadAction<PeripheryDirection | undefined>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        direction: action.payload
      }
    }),
    setNewEntityUnits: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        units: action.payload
      }
    })
  },
  selectors: defaultInventorySelectors<PeripheryType, PeripheryTypesState>()
})

export const peripheryTypesSlice = slice.injectInto(rootReducer)
