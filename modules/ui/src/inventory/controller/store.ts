import { ControllersState, NewController } from './types'
import { createSlice } from '@reduxjs/toolkit'
import { defaultInventoryActions, defaultInventorySelectors } from '../store-mixin'
import type { Controller } from '../../types'
import { rootReducer } from '../../store/root-store'

const initialState: ControllersState = {
  knownEntities: [],
  isLoading: true
}

const emptyNewEntity: NewController = { canBeSaved: false }

const controllerStore = createSlice({
  name: 'controllers',
  initialState,
  reducers: {
    ...defaultInventoryActions<Controller, ControllersState>(emptyNewEntity)
  },
  selectors: defaultInventorySelectors<Controller, ControllersState>()
})

export const controllersSlice = controllerStore.injectInto(rootReducer)
