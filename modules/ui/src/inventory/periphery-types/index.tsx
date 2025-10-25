import React from 'react'
import { createListener } from './listener'
import { initFor, RegisterCallbacks } from '../../utils/init-lazy'
import { addNewEntity, setEntities, setInitialized, setLoading } from './actions'
import { getIsInitialized } from '../controller/selectors'
import { InnerList } from './list'
import { InnerForm } from './form'

createListener()

const registerCallbacks = (reg: RegisterCallbacks) => {
  reg('periphery-type', addNewEntity)
  reg('periphery-types', setEntities)
}

export const InitPeripheryTypes = initFor(
  'get-periphery-types',
  getIsInitialized,
  setInitialized,
  setLoading,
  registerCallbacks
)

export const List = () => (
  <>
    <InitPeripheryTypes />
    <InnerList />
  </>
)

export const Form = () => (
  <>
    <InitPeripheryTypes />
    <InnerForm />
  </>
)
