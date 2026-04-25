import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { PeripheryTypesState, NewPeripheryType } from './types'
import { rootReducer } from '../../store/root-store'
import type { PeripheryConnection, PeripheryDirection } from '../../types'
import {
  defaultInventoryActions,
  defaultInventorySelectors,
  NewEntityPayload
} from '../store-mixin'
import { act } from 'react'

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
    saveNewConnection: (state) => ({
      ...state,
      newConnection: undefined,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        connections: {
          ...state.newEntity?.connections || {},
          [state.newConnection?.name || '']: { ...(state.newConnection || {}), canBeSaved: undefined }
        }
      }
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
    addNewConnection: (state) => ({
      ...state,
      newConnection: { canBeSaved: false }
    }),
    setNewConnection: (state, action: PayloadAction<PeripheryConnection>) => ({
      ...state,
      newConnection: !!action.payload ? { canBeSaved: false } : action.payload
    }),
    cancelNewConnection: (state) => ({
      ...state,
      newConnection: undefined
    }),
    setNewConnectionCanBeSaved: (state, action: PayloadAction<boolean>) => ({
      ...state,
      newConnection: {
        ...(state.newConnection || { canBeSaved: action.payload }),
        canBeSaved: action.payload
      }
    }),
    setNewConnectionName: (state, action: PayloadAction<string>) => ({
      ...state,
      newConnection: {
        ...(state.newConnection || { canBeSaved: false }),
        name: action.payload,
      }
    }),
    setNewConnectionDirection: (state, action: PayloadAction<PeripheryDirection>) => ({
      ...state,
      newConnection: {
        ...(state.newConnection || { canBeSaved: false }),
        direction: action.payload,
      }
    }),
    setNewConnectionUnits: (state, action: PayloadAction<string>) => ({
      ...state,
      newConnection: {
        ...(state.newConnection || { canBeSaved: false }),
        units: action.payload,
      }
    }),
    setNewConnectionType: (state, action: PayloadAction<string>) => ({
      ...state,
      newConnection: {
        ...(state.newConnection || { canBeSaved: false }),
        type: action.payload,
      }
    })
  },
  selectors: defaultInventorySelectors(emptyNewEntity)
})



export const peripheryTypesSlice = slice.injectInto(rootReducer)
