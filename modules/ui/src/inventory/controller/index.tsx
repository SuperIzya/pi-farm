import React from 'react'
import { createListener } from './listener'

import { addNewEntity, setEntities, setInitialized, setLoading } from './actions'

import { InnerForm } from './form'
import { InnerList } from './list'
import { getIsInitialized } from './selectors'
import { initFor, RegisterCallbacks } from '../../utils/init-lazy'
import { InitControllerTypes } from '../controller-types'

createListener()

const registerCallbacks = (reg: RegisterCallbacks) => {
  reg('controller', addNewEntity)
  reg('controllers', setEntities)
}
const InitOnly = initFor(
  'get-controllers',
  getIsInitialized,
  setInitialized,
  setLoading,
  registerCallbacks
)

const InitController = () => (
  <>
    <InitControllerTypes />
    <InitOnly />
  </>
)

export const List = () => (
  <>
    <InitController />
    <InnerList />
  </>
)
export const Form = () => (
  <>
    <InitController />
    <InnerForm />
  </>
)
