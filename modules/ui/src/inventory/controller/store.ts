import type { ControllersState, NewController, ProcessConsoleState } from './types'
import { createSlice, type PayloadAction } from '@reduxjs/toolkit'
import {
  defaultInventoryActions,
  defaultInventorySelectors,
  NewEntityPayload
} from '../store-mixin'
import type { IdType, ProcessLog, ProcessState } from '../../types'
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
    setNewEntityDescription: (state: ControllersState, action: NewEntityPayload<string>) => ({
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

const consoleInitialState: ProcessConsoleState = {
  isOpen: false,
  logs: []
}

export const processConsoleSlice = createSlice({
  name: 'processConsole',
  initialState: consoleInitialState,
  reducers: {
    openConsole: state => ({ ...state, isOpen: true }),
    closeConsole: () => consoleInitialState,
    receiveProcessLog: (state, { payload }: PayloadAction<ProcessLog>) =>
      state.isOpen ? { ...state, logs: [...state.logs, payload].slice(-200) } : state,
    receiveProcessState: (state, { payload }: PayloadAction<ProcessState>) => ({
      ...state,
      process: payload
    })
  },
  selectors: {
    getIsOpen: state => state.isOpen,
    getLogs: state => state.logs,
    getProcess: state => state.process
  }
}).injectInto(rootReducer)

export const { openConsole, closeConsole, receiveProcessLog, receiveProcessState } =
  processConsoleSlice.actions
export const { getIsOpen, getLogs, getProcess } = processConsoleSlice.selectors
