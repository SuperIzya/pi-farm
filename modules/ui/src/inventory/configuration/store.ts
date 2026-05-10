import type {
  ConfigurationGraph,
  ConfigurationsState,
  ControllerNode,
  GraphEdge,
  ProcessingNode,
  ProcessingUnitsState
} from './types'
import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { defaultInventoryActions, defaultInventorySelectors } from '../store-mixin'
import type { ProcessingUnit, ControllerId, NewEntity } from '../../types'
import { rootReducer } from '../../store/root-store'

const initialConfigurationState: ConfigurationsState = {
  knownEntities: [],
  isLoading: true,
  isInitialized: false
}

const emptyNewEntity: NewEntity<ConfigurationGraph> = { canBeSaved: false }

const configurationsStore = createSlice({
  name: 'configurations',
  initialState: initialConfigurationState,
  reducers: {
    ...defaultInventoryActions(emptyNewEntity),
    resetGraph: state => ({ ...state, newEntity: emptyNewEntity }),
    setName: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity ?? emptyNewEntity),
        name: action.payload
      }
    }),
    setDescription: (state, action: PayloadAction<string | undefined>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity ?? emptyNewEntity),
        description: action.payload
      }
    }),
    removeConnection: (state, action: PayloadAction<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity ?? emptyNewEntity),
        edges: (state.newEntity?.edges ?? []).filter(edge => edge.id !== action.payload)
      }
    }),
    removeControllerNode: (state, action: PayloadAction<ControllerId>) => {
      const { [action.payload]: _, ...restControllers } = state.newEntity?.controllers || {}
      return {
        ...state,
        newEntity: {
          ...(state.newEntity ?? emptyNewEntity),
          controllers: restControllers
        }
      }
    },
    addControllerNode: (state, action: PayloadAction<ControllerNode>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity ?? emptyNewEntity),
        controllers: {
          ...state.newEntity?.controllers,
          [action.payload.data.controllerId]: action.payload
        }
      }
    }),
    removeProcessorNode: (state, action: PayloadAction<string>) => {
      const { [action.payload]: _, ...restProcessingUnits } = state.newEntity?.processingUnits || {}
      return {
        ...state,
        newEntity: {
          ...(state.newEntity ?? emptyNewEntity),
          processingUnits: restProcessingUnits
        }
      }
    },
    addProcessorNode: (state, action: PayloadAction<ProcessingNode>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity ?? emptyNewEntity),
        processingUnits: {
          ...state.newEntity?.processingUnits,
          [action.payload.data.processingUnitId]: action.payload
        }
      }
    }),
    addEdge: (state, action: PayloadAction<GraphEdge>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity ?? emptyNewEntity),
        edges: [...(state.newEntity?.edges ?? []), action.payload]
      }
    })
  },
  selectors: {
    ...defaultInventorySelectors(emptyNewEntity),
    getEdges: ({ newEntity }) => newEntity?.edges ?? [],
    getControllers: ({ newEntity }) => newEntity?.controllers ?? {},
    getProcessingUnits: ({ newEntity }) => newEntity?.processingUnits ?? {}
  }
})

const initialProcessingUnitsState: ProcessingUnitsState = {
  isInitialized: false,
  isLoading: true,
  entities: {}
}

const processingUnitsStore = createSlice({
  name: 'processingUnits',
  initialState: initialProcessingUnitsState,
  reducers: {
    setProcessingUnits: (state: ProcessingUnitsState, action: PayloadAction<ProcessingUnit[]>) => ({
      ...state,
      entities: action.payload.reduce((acc, pu) => ({ ...acc, [pu.name]: pu }), {}),
      isLoading: false
    }),
    addProcessingUnit: (state: ProcessingUnitsState, action: PayloadAction<ProcessingUnit>) => ({
      ...state,
      entities: {
        ...state.entities,
        [action.payload.name]: action.payload
      }
    }),
    setProcessingUnitsIsLoading: (state: ProcessingUnitsState, action: PayloadAction<boolean>) => ({
      ...state,
      isLoading: action.payload
    }),
    setProcessingUnitsInitialized: (state: ProcessingUnitsState) => ({
      ...state,
      isInitialized: true
    })
  },
  selectors: {
    getProcessingUnits: ({ entities }) => entities,
    getProcessingUnitsIsLoading: ({ isLoading }) => isLoading,
    getProcessingUnitsInitialized: ({ isInitialized }) => isInitialized
  }
})

export const processingUnitsSlice = processingUnitsStore.injectInto(rootReducer)

export const configurationsSlice = configurationsStore.injectInto(rootReducer)
