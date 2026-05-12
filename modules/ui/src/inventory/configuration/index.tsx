import React from 'react'
import { createListener } from './listener'
import { initFor, RegisterCallbacks } from '../../utils/init-lazy'
import {
  addNewEntity,
  addProcessingUnit,
  setEntities,
  setInitialized,
  setLoading,
  setProcessingUnits,
  setProcessingUnitsInitialized,
  setProcessingUnitsIsLoading
} from './actions'
import { getIsInitialized, getProcessingUnitsInitialized } from './selectors'
import { InitController } from '../controller'
import { InnerList } from './list'
import { InnerForm } from './form'
import { Configuration } from '../../types'

createListener()

const transformConfigurationToGraph = (c: Configuration) => ({
  id: c.id,
  name: c.name,
  description: c.description,
  processingUnits: c.processingUnits.reduce((acc, puId) => ({ ...acc, [puId]: { id: puId } }), {}),
  edges: c.connections.map(c => ({
    data: c,
    id: `${c.from}-${c.to}`,
    source: 'controllerId' in c.from ? c.from.controllerId.toString() : c.from.processingUnitId,
    target: 'controllerId' in c.to ? c.to.controllerId.toString() : c.to.processingUnitId
  })),
  controllers: c.connections.reduce(
    (acc, c) => ({
      ...acc,
      ...('controllerId' in c.to ? { [c.to.controllerId]: c.to.controllerId } : {}),
      ...('controllerId' in c.from ? { [c.from.controllerId]: c.from.controllerId } : {})
    }),
    {}
  )
})

const setConfigurations = (data: Configuration[]) =>
  setEntities(data.map(transformConfigurationToGraph))
const setNewConfiguration = (data: Configuration) =>
  addNewEntity(transformConfigurationToGraph(data))

const InitOnlyConfigurations = initFor(
  'get-configurations',
  getIsInitialized,
  setInitialized,
  setLoading,
  (reg: RegisterCallbacks) => {
    reg('configuration', setNewConfiguration)
    reg('configurations', setConfigurations)
  }
)

const InitOnlyProcessingUnits = initFor(
  'get-processing-units',
  getProcessingUnitsInitialized,
  setProcessingUnitsInitialized,
  setProcessingUnitsIsLoading,
  (reg: RegisterCallbacks) => {
    reg('processing-units', setProcessingUnits)
    reg('processing-unit', addProcessingUnit)
  }
)

const InitConfiguration = () => (
  <>
    <InitController />
    <InitOnlyProcessingUnits />
    <InitOnlyConfigurations />
  </>
)

export const List = () => (
  <>
    <InitConfiguration />
    <InnerList />
  </>
)

export const Form = () => (
  <>
    <InitConfiguration />
    <InnerForm />
  </>
)
