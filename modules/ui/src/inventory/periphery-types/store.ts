import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { PeripheryTypesState, NewPeripheryType, NewConnection } from './types'
import { rootReducer } from '../../store/root-store'
import type { PeripheryConnection, PeripheryDirection, PeripheryType } from '../../types'
import {
  defaultInventoryActions,
  defaultInventorySelectors,
  NewEntityPayload
} from '../store-mixin'
import { addNewConnection } from './actions'

type ConnectionChange = {
  newEntity?: Partial<PeripheryType> & { canBeSaved: boolean }
  newConnection: NewConnection
}

const NoSave = { canBeSaved: false } as const

const canBeSaved = <T extends Partial<T> & {canBeSaved: boolean}>(entity?: T): entity is T & { canBeSaved: true } =>  entity !== undefined && entity.canBeSaved

const initialState: PeripheryTypesState = {
  knownEntities: [],
  isLoading: true,
  isInitialized: false,
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
    addNewConnection: (state) => ({
      ...state,
      newConnection: { canBeSaved: false }
    }),
    editConnection: (state, action: PayloadAction<number>) => {

        if(state.newEntity === undefined || state.newEntity.connections === undefined || state.newEntity.connections.length <= action.payload)
          return state

        const newConnection = {
          ...state.newEntity.connections[action.payload],
          canBeSaved: true
        }

        if(state.newConnection === undefined) {
          return {
            ...state,
            newConnection
          }
        }

        if(!state.newConnection.canBeSaved) return state
        

        return {
        ...state,
        newEntity: {
            ...state.newEntity,
            connections: state.newEntity.connections.filter((_, index) => index !== action.payload)
        },
        
        newConnection
      }
    },
    cancelConnection: (state) => ({
      ...state,
      newConnection: undefined
    }),
    setConnectionCanBeSaved: (state, action: PayloadAction<boolean>) => ({
      ...state,
      newConnection: {
        ...(state.newConnection || { canBeSaved: action.payload }),
        canBeSaved: action.payload
      }
    }),
    setConnectionName: (state, action: NewEntityPayload<string>) => ({
      ...state,
      newConnection: {
        ...(state.newConnection || NoSave),
        name: action.payload
      } 
    }),
    setConnectionDirection: (state, action: NewEntityPayload<PeripheryDirection>) => ({
      ...state,
      newConnection: {
        ...(state.newConnection || NoSave),
        direction: action.payload
      }
    }),
    setConnectionUnits: (state, action: NewEntityPayload<string>) => ({
      ...state,
      newConnection: {
        ...(state.newConnection || NoSave),
        units: action.payload
      }
    }),
    setConnectionType: (state, action: NewEntityPayload<string>) => ({
      ...state,
      newConnection: {
        ...(state.newConnection || NoSave),
        type: action.payload
      }
    }),
    saveConnection: (state) => ({
      ...state,
      newConnection: undefined,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        connections: [
          canBeSaved(state.newConnection) && state.newConnection,
          ...state.newEntity?.connections ?? []
        ].filter(Boolean) as PeripheryConnection[]
      }
    })
  },
  selectors: {
     ...defaultInventorySelectors(emptyNewEntity),
     getConnection: (state: PeripheryTypesState) => state.newConnection
  }
})

export const peripheryTypesSlice = slice.injectInto(rootReducer)
