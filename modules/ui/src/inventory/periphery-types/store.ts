import { createSlice } from '@reduxjs/toolkit'
import { PeripheryTypesState, NewPeripheryType } from './types'
import { rootReducer } from '../../store/root-store'
import type { PeripheryDirection } from '../../types'
import {
  defaultInventoryActions,
  defaultInventorySelectors,
  NewEntityPayload
} from '../store-mixin'

const initialState: PeripheryTypesState = {
  knownEntities: [],
  isLoading: true,
  isInitialized: false
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
    setNewEntityImage: (state, action: NewEntityPayload<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        image: action.payload
      }
    }),
    setNewEntityDirection: (state, action: NewEntityPayload<PeripheryDirection>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        direction: action.payload
      }
    }),
    setNewEntityUnits: (state, action: NewEntityPayload<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        units: action.payload
      }
    })
  },
  selectors: defaultInventorySelectors(emptyNewEntity)
})

export const peripheryTypesSlice = slice.injectInto(rootReducer)
