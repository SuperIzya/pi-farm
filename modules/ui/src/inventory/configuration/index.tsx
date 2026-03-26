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

createListener()

const InitOnlyConfigurations = initFor(
  'get-configurations',
  getIsInitialized,
  setInitialized,
  setLoading,
  (reg: RegisterCallbacks) => {
    reg('configuration', addNewEntity)
    reg('configurations', setEntities)
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
