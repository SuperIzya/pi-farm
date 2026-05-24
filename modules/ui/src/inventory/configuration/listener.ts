import {
  rootListener,
  startListeningCanSaveMemo,
  startListeningSaveMemo,
  TransformFunction
} from '../../store/listeners'
import type { ConfigurationGraph, ProcessorEndpoint, RootState } from './types'
import { getNewEntity } from './selectors'
import {
  setNewEntityCanBeSaved,
  saveNewEntity,
  setLoading,
  setName,
  setDescription,
  addEdge,
  removeEdge,
  addControllerNode,
  addProcessorNode,
  removeControllerNode,
  removeProcessorNode,
  editEntity,
  setEditGraph
} from './actions'
import type {
  New,
  Configuration,
  ProcessorAddress,
  IdType,
  ControllerId,
  Connection,
  Controller,
  ControllerType,
  ControllerTypeId,
  PeripheryTypeId,
  PeripheryType,
  FlowDirection
} from '../../types'
import { createSelector, isAnyOf, PayloadAction } from '@reduxjs/toolkit'
import { XYPosition } from '@xyflow/react'
import { toConfigurationGraph } from './transformation'
import { isFromProcessor, isToProcessor } from '../../types/tests'

const toNoId = (entity: Partial<ConfigurationGraph>): New<Configuration> => {
  const edges = entity.edges?.map(({ data }) => data).filter(e => e !== undefined) ?? []

  return {
    name: entity.name || '',
    description: entity.description || '',
    graphData: {
      controllers: Object.entries(entity.controllers ?? {}).reduce(
        (acc, [id, data]) => ({
          ...acc,
          [id]: {
            position: data?.position || { x: 0, y: 0 }
          }
        }),
        {} as Record<ControllerId, { position: XYPosition }>
      ),
      processingUnits: (entity.processingUnits ?? []).reduce(
        (acc, data) => ({
          ...acc,
          [data.data.id]: {
            position: data.position || { x: 0, y: 0 }
          }
        }),
        {} as Record<string, { position: XYPosition }>
      )
    },
    processors: (entity.processingUnits ?? []).map((data, idx) => ({
      unit: data.data.unit || '',
      graphId: data.data.id,
      parameters: data.data.parameters || {},
      inbound: edges
        .filter(e => isToProcessor(e))
        .filter(e => e.to.id === data.data.id)
        .map(e => ({
          controllerId: e.from.controllerId,
          peripheryId: e.from.peripheryId,
          name: e.to.name
        })),
      outbound: edges
        .filter(e => isFromProcessor(e))
        .filter(e => e.from.id === data.data.id)
        .map(e => ({
          controllerId: e.to.controllerId,
          peripheryId: e.to.peripheryId,
          name: e.from.name
        }))
    }))
  }
}

const transformSave: TransformFunction<
  Configuration,
  'save-configuration',
  New<Configuration>,
  'update-configuration',
  ConfigurationGraph
> = entity =>
  'id' in entity
    ? {
        hasId: true,
        data: {
          ...toNoId(entity),
          id: entity.id || 0
        }
      }
    : {
        hasId: false,
        data: toNoId(entity)
      }

type TransformedConfig = ReturnType<typeof transformSave>

const allInputEdgesSelector = createSelector(getNewEntity, newEntity =>
  (newEntity?.edges ?? [])
    .map(({ data }) => data)
    .filter(data => data !== undefined)
    .flatMap(data =>
      data.to !== undefined && 'unit' in data.to ? [data.to as ProcessorAddress] : []
    )
    .reduce(
      (acc, to) => ({
        ...acc,
        [to.id]: [...(acc[to.id] ?? []), to.name]
      }),
      {} as Record<string, string[]>
    )
)

const allProcessorsInputsSelector = createSelector(getNewEntity, newEntity =>
  Object.values(newEntity?.processingUnits ?? {})
    .flatMap(({ data }) =>
      data.endpoints
        .filter(endpoint => endpoint.direction === 'in')
        .map(endpoint => ({ id: data.id, name: endpoint.name }))
    )
    .reduce(
      (acc, { id, name }) => ({
        ...acc,
        [id]: [...(acc[id] ?? []), name]
      }),
      {} as Record<string, string[]>
    )
)

const isNewEntityCanBeSavedSelector = createSelector(
  getNewEntity,
  allInputEdgesSelector,
  allProcessorsInputsSelector,
  (newEntity, allInputEdges, allProcessorsInputs) => {
    if (
      newEntity === undefined
      || newEntity.name === undefined
      || newEntity.name === ''
      || newEntity.edges === undefined
      || newEntity.processingUnits === undefined
      || newEntity.controllers === undefined
    )
      return false

    if (
      !Object.entries(allProcessorsInputs).every(
        ([processorId, inputNames]) =>
          allInputEdges[processorId] !== undefined
          && inputNames.every(n => allInputEdges[processorId].includes(n))
      )
    )
      return false

    return transformSave(newEntity)
  }
)

export const puConnectionToEndpoint =
  (processingUnitId: string, direction: FlowDirection) =>
  ({ name, type, units }: Connection): ProcessorEndpoint => ({
    name,
    units,
    type,
    direction,
    processor: { name, unit: processingUnitId, id: processingUnitId }
  })

export const createListener = () => {
  startListeningCanSaveMemo<RootState, TransformedConfig>(
    setName,
    setDescription,
    addEdge,
    removeEdge,
    addControllerNode,
    addProcessorNode,
    removeControllerNode,
    removeProcessorNode
  )(isNewEntityCanBeSavedSelector, getNewEntity, setNewEntityCanBeSaved)

  startListeningSaveMemo<RootState>()(
    isNewEntityCanBeSavedSelector,
    saveNewEntity,
    setLoading,
    'save-configuration',
    'update-configuration'
  )

  rootListener.startListening({
    matcher: isAnyOf(editEntity),
    effect: ({ payload }: PayloadAction<IdType>, listenerApi) => {
      const state = listenerApi.getState() as RootState
      const newEntity = state.configurations.knownEntities.find(e => e.id === payload)
      if (!newEntity) return

      const graph: ConfigurationGraph = toConfigurationGraph(newEntity, {
        units: state.processingUnits.entities,
        controllers: state.controllers.knownEntities.reduce(
          (acc, ctl) => ({
            ...acc,
            [ctl.id]: ctl
          }),
          {} as Record<ControllerId, Controller>
        ),
        controllerTypes: state.controllerTypes.knownEntities.reduce(
          (acc, type) => ({
            ...acc,
            [type.id]: type
          }),
          {} as Record<ControllerTypeId, ControllerType>
        ),
        peripheryTypes: state.periphery.knownEntities.reduce(
          (acc, type) => ({
            ...acc,
            [type.id]: type
          }),
          {} as Record<PeripheryTypeId, PeripheryType>
        )
      })

      listenerApi.dispatch(setEditGraph(graph))
    }
  })
}
