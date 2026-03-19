import React from 'react'
import { createListener } from './listener'
import { initFor, RegisterCallbacks } from '../../utils/init-lazy'
import {
  addNewEntity,
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
  (req: RegisterCallbacks) => req('processing-units', setProcessingUnits)
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
