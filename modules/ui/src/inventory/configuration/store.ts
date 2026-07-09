import type {
  ConfigurationGraph,
  ConfigurationsState,
  ControllerNode,
  GraphEdge,
  ProcessingNode,
  ProcessingUnitsState
} from './types'
import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import type { ProcessingUnit, ControllerId, NewEntity, Configuration, IdType } from '../../types'
import { rootReducer } from '../../store/root-store'

const initialConfigurationState: ConfigurationsState = {
  knownEntities: [],
  isLoading: true,
  isInitialized: false
}

type SetParameters = {
  id: string
  parameters: Record<string, unknown>
}
const emptyNewEntity: NewEntity<ConfigurationGraph> = { canBeSaved: false }

const configurationsStore = createSlice({
  name: 'configurations',
  initialState: initialConfigurationState,
  reducers: {
    setLoading: (state, action: PayloadAction<boolean>) => ({
      ...state,
      isLoading: action.payload
    }),
    setEntities: (state, action: PayloadAction<Configuration[]>) => ({
      ...state,
      knownEntities: action.payload,
      isLoading: false
    }),
    setInitialized: state => ({
      ...state,
      isInitialized: true
    }),
    setNewEntityCanBeSaved: (state, action: PayloadAction<boolean>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        canBeSaved: action.payload
      }
    }),
    startNewEntity: state => ({
      ...state,
      newEntity: emptyNewEntity,
      editingIndex: undefined
    }),
    saveNewEntity: state => state,
    setSvgPreview: (state, action: PayloadAction<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity ?? emptyNewEntity),
        svg: action.payload
      }
    }),
    cancelNewEntity: state => ({
      ...state,
      newEntity: undefined,
      editingId: undefined
    }),
    addNewEntity: (state: ConfigurationsState, action: PayloadAction<Configuration>) => {
      const index = state.knownEntities.findIndex(e => e.id === action.payload.id)
      if (index === -1) {
        return {
          ...state,
          knownEntities: [action.payload, ...state.knownEntities],
          isLoading: false
        }
      }

      const before = state.knownEntities.slice(0, index)
      const after = state.knownEntities.slice(index + 1)
      return {
        ...state,
        knownEntities: [...before, action.payload, ...after],
        isLoading: false
      }
    },
    editEntity: (state, _: PayloadAction<IdType>) => state,
    setEditGraph: (state, action: PayloadAction<ConfigurationGraph>) => ({
      ...state,
      newEntity: {
        ...action.payload,
        canBeSaved: true
      },
      editingIndex: action.payload.id
    }),
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
    addEdge: (state, action: PayloadAction<GraphEdge>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity ?? emptyNewEntity),
        edges: [...(state.newEntity?.edges ?? []), action.payload]
      }
    }),
    selectEdge: (state, action: PayloadAction<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity ?? emptyNewEntity),
        edges: (state.newEntity?.edges ?? []).map(edge => ({
          ...edge,
          selected: edge.id === action.payload ? !edge.selected : edge.selected
        }))
      }
    }),
    removeEdge: (state, action: PayloadAction<string>) => ({
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
          [action.payload.data.id]: action.payload
        }
      }
    }),
    removeProcessorNode: (state, action: PayloadAction<string>) => {
      const restProcessingUnits = (state.newEntity?.processingUnits || []).filter(
        p => p.id !== action.payload
      )
      return {
        ...state,
        newEntity: {
          ...(state.newEntity ?? emptyNewEntity),
          processingUnits: restProcessingUnits
        }
      }
    },
    setProcessorParams: (state, action: PayloadAction<SetParameters>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity ?? emptyNewEntity),
        processingUnits: (state.newEntity?.processingUnits ?? []).map(p =>
          p.id === action.payload.id
            ? { ...p, data: { ...p.data, parameters: action.payload.parameters } }
            : p
        )
      }
    }),
    addProcessorNode: (state, action: PayloadAction<ProcessingNode>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity ?? emptyNewEntity),
        processingUnits: [
          ...(state.newEntity?.processingUnits ?? []),
          {
            ...action.payload,
            data: {
              ...action.payload.data,
              endpoints: action.payload.data.endpoints.map(e => ({
                ...e,
                processor: { ...e.processor, id: action.payload.data.id }
              }))
            }
          }
        ]
      }
    })
  },
  selectors: {
    getKnownEntities: ({ knownEntities }) => knownEntities,
    getNewEntity: ({ newEntity }) => newEntity,
    getIsLoading: ({ isLoading }) => isLoading,
    getIsInitialized: ({ isInitialized }) => isInitialized,
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
