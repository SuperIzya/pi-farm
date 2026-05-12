import { startListeningCanSaveMemo, startListeningSaveMemo, TransformFunction } from '../../store/listeners'
import type { ConfigurationGraph, RootState } from './types'
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
  removeProcessorNode
} from './actions'
import type { New, Configuration, ProcessorAddress } from '../../types'
import { createSelector } from '@reduxjs/toolkit'

const toNoId = (entity: Partial<ConfigurationGraph>): New<Configuration> => ({
  name: entity.name || '',
  description: entity.description || '',
  processingUnits: Object.keys(entity.processingUnits ?? {}),
  connections: entity.edges?.map(({ data }) => data).filter(c => c !== undefined) || []
})

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

const allInputEdgesSelector = createSelector(
  getNewEntity, 
  newEntity =>
    (newEntity?.edges ?? [])
      .map(({ data }) => data)    
      .filter(data => data !== undefined)
      .flatMap(data => data.to !== undefined && 'processingUnitId' in data.to ? [data.to as ProcessorAddress] : [])
      .reduce((acc, to) => ({
        ...acc, 
        [to.processingUnitId]: [
          ...(acc[to.processingUnitId] ?? []),
          to.name
        ]
      }), {} as Record<string, string[]>)
)

const allProcessorsInputsSelector = createSelector(
  getNewEntity, 
  newEntity => 
    Object.values(newEntity?.processingUnits ?? {})
      .flatMap(({data}) => data.endpoints
        .filter(endpoint => endpoint.direction === 'in')
        .map(endpoint => ({ id: data.id, name: endpoint.name }))
      ).reduce((acc, { id, name }) => ({
        ...acc,
        [id]: [...(acc[id] ?? []), name]
      }), {} as Record<string, string[]>)
)

const isNewEntityCanBeSavedSelector = createSelector(
  getNewEntity,
  allInputEdgesSelector,
  allProcessorsInputsSelector,
  (newEntity, allInputEdges, allProcessorsInputs) => {
  if (
      newEntity === undefined ||
      newEntity.name === undefined ||
      newEntity.name === '' ||
      newEntity.edges === undefined ||
      newEntity.processingUnits === undefined ||
      newEntity.controllers === undefined
    ) return false

    if(!Object.entries(allProcessorsInputs).every(([processorId, inputNames]) =>
      allInputEdges[processorId] !== undefined &&
        inputNames.every(n => allInputEdges[processorId].includes(n))
    )) return false

    return transformSave(newEntity)
  }
)
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
  )(
    isNewEntityCanBeSavedSelector,
    getNewEntity,
    setNewEntityCanBeSaved
  )

  startListeningSaveMemo<RootState>()(
    isNewEntityCanBeSavedSelector,
    saveNewEntity,
    setLoading,
    'save-configuration',
    'update-configuration'
  )
}
