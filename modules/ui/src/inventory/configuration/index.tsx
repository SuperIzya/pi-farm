import React from 'react'
import { createListener } from './listener'
import { initFor, RegisterCallbacks } from '../../utils/init-lazy'
import { addNewEntity, setEntities, setInitialized, setLoading } from './actions'
import { getIsInitialized } from './selectors'
import { InitController } from '../controller'
import { InnerList } from './list'

createListener()

const registerCallbacks = (reg: RegisterCallbacks) => {
  reg('configuration', addNewEntity)
  reg('configurations', setEntities)
}

const InitOnly = initFor(
  'get-configurations',
  getIsInitialized,
  setInitialized,
  setLoading,
  registerCallbacks
)

const InitConfiguration = () => (
  <>
    <InitController />
    <InitOnly />
  </>
)

export const List = () => (
  <>
    <InitConfiguration />
    <InnerList />
  </>
)
export const Form = () => <InitConfiguration />
