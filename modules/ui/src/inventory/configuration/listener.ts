import {
  rootListener,
  startListeningCanSaveMemo,
  startListeningSaveMemo,
  TransformFunction
} from '../../store/listeners'
import type { ConfigurationGraph, ProcessorEndpoint, RootState } from './types'
import { getAllProcessingUnits, getNewEntity } from './selectors'
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
  setEditGraph,
  setProcessorParams
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
  FlowDirection,
  FieldType
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
      ),
      svg: entity.svg || ''
    },
    processors: (entity.processingUnits ?? []).map(({ data }) => ({
      unit: data.unit || '',
      graphId: data.id,
      parameters: data.parameters || {},
      inbound: edges
        .filter(e => isToProcessor(e))
        .filter(e => e.to.id === data.id)
        .map(e => ({
          controllerId: e.from.controllerId,
          peripheryName: e.from.peripheryName,
          peripheryChannel: e.from.peripheryChannel,
          processorConnectionName: e.to.name
        })),
      outbound: edges
        .filter(e => isFromProcessor(e))
        .filter(e => e.from.id === data.id)
        .map(e => ({
          controllerId: e.to.controllerId,
          peripheryName: e.to.peripheryName,
          peripheryChannel: e.to.peripheryChannel,
          processorConnectionName: e.from.name
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
> = entity => {
  const noId = toNoId(entity)
  return 'id' in entity
    ? {
        hasId: true,
        data: {
          ...noId,
          previewSvg: entity.svg || '',
          id: entity.id || 0
        }
      }
    : {
        hasId: false,
        data: noId
      }
}

type TransformedConfig = ReturnType<typeof transformSave>

const getAllSchemas = createSelector(getAllProcessingUnits, processingUnits =>
  Object.values(processingUnits).reduce(
    (acc, pu) => ({ ...acc, [pu.name]: pu.paramsSchema }),
    {} as Record<string, Record<string, FieldType>>
  )
)

const allEdgesSelector = createSelector(getNewEntity, newEntity =>
  (newEntity?.edges ?? [])
    .map(({ data }) => data)
    .filter(data => data !== undefined)
    .flatMap(data =>
      data.to !== undefined && 'unit' in data.to
        ? [data.to as ProcessorAddress]
        : data.from !== undefined && 'unit' in data.from
          ? [data.from as ProcessorAddress]
          : []
    )
    .reduce(
      (acc, to) => ({
        ...acc,
        [to.id]: [...(acc[to.id] ?? []), to.name]
      }),
      {} as Record<string, string[]>
    )
)

const allProcessorsConnectionsSelector = createSelector(getNewEntity, newEntity =>
  Object.values(newEntity?.processingUnits ?? {})
    .flatMap(({ data }) => data.endpoints.map(endpoint => ({ id: data.id, name: endpoint.name })))
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
  getAllSchemas,
  allEdgesSelector,
  allProcessorsConnectionsSelector,
  (newEntity, allSchemas, allEdges, allProcessorsConnections) => {
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
      !Object.entries(allProcessorsConnections).every(
        ([processorId, names]) =>
          allEdges[processorId] !== undefined && names.every(n => allEdges[processorId].includes(n))
      )
    )
      return false

    /* No parameter schema validation since no parameter form yet    
    
    if (
      !newEntity.processingUnits.every(pu =>
        validateParams(allSchemas[pu.data.unit] ?? {}, pu.data.parameters)
      )
    )
      return false */

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
    removeProcessorNode,
    setProcessorParams
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
