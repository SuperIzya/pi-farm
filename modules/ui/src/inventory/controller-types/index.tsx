import React from 'react'
import { createListener } from './listener'
import { initFor, RegisterCallbacks } from '../../utils/init-lazy'
import { addNewEntity, setEntities, setInitialized, setLoading } from './actions'
import { getIsInitialized } from './selectors'
import { InitPeripheryTypes } from '../periphery-types'

import { InnerList } from './list'
import { InnerForm } from './form'

createListener()

const registerCallbacks = (reg: RegisterCallbacks) => {
  reg('controller-type', addNewEntity)
  reg('controller-types', setEntities)
}

const InitOnly = initFor(
  'get-controller-types',
  getIsInitialized,
  setInitialized,
  setLoading,
  registerCallbacks
)

export const InitControllerTypes = () => (
  <>
    <InitPeripheryTypes />
    <InitOnly />
  </>
)

export const List = () => (
  <>
    <InitControllerTypes />
    <InnerList />
  </>
)

export const Form = () => (
  <>
    <InitControllerTypes />
    <InnerForm />
  </>
)
