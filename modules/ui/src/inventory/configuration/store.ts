import { ConfigurationsState, NewConfiguration } from './types'
import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import {
  defaultInventoryActions,
  defaultInventorySelectors,
  NewEntityPayload
} from '../store-mixin'
import type { IdType, ConfigurationEndpoint } from '../../types'
import { rootReducer } from '../../store/root-store'
import { Edge, Node } from '@xyflow/react'

const initialState: ConfigurationsState = {
  knownEntities: [],
  isLoading: true,
  isInitialized: false
}

const emptyNewEntity: NewConfiguration = { canBeSaved: false }

const configurationStore = createSlice({
  name: 'configurations',
  initialState,
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

export const configurationsSlice = configurationStore.injectInto(rootReducer)
