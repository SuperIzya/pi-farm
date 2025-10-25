import { ControllersState, NewController } from './types'
import { createSlice } from '@reduxjs/toolkit'
import {
  defaultInventoryActions,
  defaultInventorySelectors,
  NewEntityPayload
} from '../store-mixin'
import type { IdType } from '../../types'
import { rootReducer } from '../../store/root-store'

const initialState: ControllersState = {
  knownEntities: [],
  isLoading: true,
  isInitialized: false
}

const emptyNewEntity: NewController = { canBeSaved: false }

const controllerStore = createSlice({
  name: 'controllers',
  initialState,
  reducers: {
    ...defaultInventoryActions(emptyNewEntity),
    setNewEntityName: (state: ControllersState, action: NewEntityPayload<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        name: action.payload
      }
    }),
    setNewEntityDescription: (
      state: ControllersState,
      action: NewEntityPayload<string>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        description: action.payload
      }
    }),
    setNewEntityTypeId: (state: ControllersState, action: NewEntityPayload<IdType>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        typeId: action.payload
      }
    })
  },
  selectors: defaultInventorySelectors(emptyNewEntity)
})

export const controllersSlice = controllerStore.injectInto(rootReducer)
