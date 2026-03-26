import {
  ConfigurationsState,
  CurrentGraph,
  NewConfiguration,
  ProcessingUnitsState
} from './types'
import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import {
  defaultInventoryActions,
  defaultInventorySelectors,
  NewEntityPayload
} from '../store-mixin'
import type {
  IdType,
  ConfigurationEndpoint,
  ProcessingUnit,
  ControllerId
} from '../../types'
import { rootReducer } from '../../store/root-store'
import { Edge, Node } from '@xyflow/react'

const initialConfigurationState: ConfigurationsState = {
  knownEntities: [],
  isLoading: true,
  isInitialized: false
}

const emptyNewEntity: NewConfiguration = { canBeSaved: false }

const configurationsStore = createSlice({
  name: 'configurations',
  initialState: initialConfigurationState,
  reducers: {
    ...defaultInventoryActions(emptyNewEntity),
    setNewEntityName: (state: ConfigurationsState, action: NewEntityPayload<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        name: action.payload
      }
    }),
    setNewEntityDescription: (
      state: ConfigurationsState,
      action: NewEntityPayload<string>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        description: action.payload
      }
    }),
    setNewEntityInputs: (
      state: ConfigurationsState,
      action: NewEntityPayload<ConfigurationEndpoint>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        inputs: action.payload
      }
    }),
    setNewEntityOutputs: (
      state: ConfigurationsState,
      action: NewEntityPayload<ConfigurationEndpoint>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        outputs: action.payload
      }
    }),
    addInputToController: (
      state: ConfigurationsState,
      action: PayloadAction<{ controllerId: IdType; peripheryId: IdType }>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        inputs: {
          ...(state.newEntity?.inputs || {}),
          [action.payload.controllerId]: [
            ...(state.newEntity?.inputs?.[action.payload.controllerId] || []),
            action.payload.peripheryId
          ]
        }
      }
    }),
    addOutputToController: (
      state: ConfigurationsState,
      action: PayloadAction<{ controllerId: IdType; peripheryId: IdType }>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        outputs: {
          ...(state.newEntity?.outputs || {}),
          [action.payload.controllerId]: [
            ...(state.newEntity?.outputs?.[action.payload.controllerId] || []),
            action.payload.peripheryId
          ]
        }
      }
    }),
    removeInputFromController: (
      state: ConfigurationsState,
      action: PayloadAction<{ controllerId: IdType; peripheryId: IdType }>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        inputs: {
          ...(state.newEntity?.inputs || {}),
          [action.payload.controllerId]: (
            state.newEntity?.inputs?.[action.payload.controllerId] || []
          ).filter((id) => id !== action.payload.peripheryId)
        }
      }
    }),
    removeOutputFromController: (
      state: ConfigurationsState,
      action: PayloadAction<{ controllerId: IdType; peripheryId: IdType }>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        outputs: {
          ...(state.newEntity?.outputs || {}),
          [action.payload.controllerId]: (
            state.newEntity?.outputs?.[action.payload.controllerId] || []
          ).filter((id) => id !== action.payload.peripheryId)
        }
      }
    }),
    setNewEntityEndpoints: (
      state: ConfigurationsState,
      action: PayloadAction<{
        inputs: ConfigurationEndpoint
        outputs: ConfigurationEndpoint
      }>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        inputs: action.payload.inputs,
        outputs: action.payload.outputs
      }
    }),
    replaceNewEntityEndpoints: (
      state: ConfigurationsState,
      action: PayloadAction<{
        inputs?: ConfigurationEndpoint
        outputs?: ConfigurationEndpoint
      }>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        inputs: action.payload.inputs,
        outputs: action.payload.outputs
      }
    }),
    setNewEntityNodes: (
      state: ConfigurationsState,
      action: PayloadAction<{
        nodes: Node[]
        edges: Edge[]
      }>
    ) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        nodes: action.payload.nodes,
        edges: action.payload.edges
      }
    }),
    setNewEntityPreview: (state: ConfigurationsState, action: PayloadAction<string>) => ({
      ...state,
      newEntity: {
        ...(state.newEntity || emptyNewEntity),
        preview: action.payload
      }
    })
  },
  selectors: defaultInventorySelectors(emptyNewEntity)
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
    setProcessingUnits: (
      state: ProcessingUnitsState,
      action: PayloadAction<ProcessingUnit[]>
    ) => ({
      ...state,
      entities: action.payload.reduce((acc, pu) => ({ ...acc, [pu.name]: pu }), {}),
      isLoading: false
    }),
    addProcessingUnit: (
      state: ProcessingUnitsState,
      action: PayloadAction<ProcessingUnit>
    ) => ({
      ...state,
      entities: {
        ...state.entities,
        [action.payload.name]: action.payload
      }
    }),
    setProcessingUnitsIsLoading: (
      state: ProcessingUnitsState,
      action: PayloadAction<boolean>
    ) => ({
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

const emptyGraph: CurrentGraph = {}

const graphStore = createSlice({
  name: 'currentGraph',
  initialState: emptyGraph,
  reducers: {
    resetGraph: () => emptyGraph,
    setSelectedControllerId: (state, action: PayloadAction<ControllerId>) => ({
      ...state,
      selectedControllerId: action.payload
    }),
    setSelectedPeripheryId: (state, action: PayloadAction<string>) => ({
      ...state,
      selectedPeripheryId: action.payload
    }),
    resetControllerId: (state) => {
      const { selectedControllerId: _, ...rest } = state
      return rest
    },
    resetPeripheryId: (state) => {
      const { selectedPeripheryId: _, ...rest } = state
      return rest
    }
  },
  selectors: {
    getSelectedControllerId: ({ selectedControllerId }) => selectedControllerId,
    getSelectedPeripheryId: ({ selectedPeripheryId }) => selectedPeripheryId
  }
})

export const processingUnitsSlice = processingUnitsStore.injectInto(rootReducer)

export const configurationsSlice = configurationsStore.injectInto(rootReducer)

export const graphSlice = graphStore.injectInto(rootReducer)
