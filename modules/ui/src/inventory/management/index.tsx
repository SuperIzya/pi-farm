import React from 'react'
import { useOnReceiveData, useSendCommand } from '../../client'
import { RouteObject } from 'react-router-dom'
import { RouteNames } from '../../utils/routes'

import { PeripheryTypesList } from './periphery-types/list'
import { PeripheryTypeForm } from './periphery-types/form'
import { ControllerTypesList } from './controller-types/list'
import { ControllerTypeForm } from './controller-types/form'
import {
  addNewEntity as addNewControllerType,
  setEntities as setControllerTypes
} from './controller-types/actions'
import {
  addNewEntity as addNewPeripheryType,
  setEntities as setPeripheryTypes
} from './periphery-types/actions'
import { createListener as createControllersListener } from './controller-types/listener'
import { createListener as createPeripheryListener } from './periphery-types/listener'
import { useDispatch } from 'react-redux'
import { setLoading } from '../../store/root-store'

export const InventoryManagement = () => {
  const [render, setRender] = React.useState(false)
  const registerCallback = useOnReceiveData()
  registerCallback('periphery-type', addNewPeripheryType)
  registerCallback('controller-type', addNewControllerType)
  registerCallback('controller-types', setControllerTypes)
  registerCallback('periphery-types', setPeripheryTypes)

  const sendCommand = useSendCommand()
  const dispatch = useDispatch()
  React.useEffect(() => {
    if (!render) {
      sendCommand('get-periphery-types')
      sendCommand('get-controller-types')
      sendCommand('get-configurations')
      dispatch(setLoading(true))
      setRender(true)
    }
  }, [])
  return null
}

const inventoryManagementRoutes = (
  path: string,
  list: React.ComponentType,
  form: React.ComponentType
): RouteObject => ({
  path,
  children: [
    {
      index: true,
      Component: list
    },
    {
      path: RouteNames.new,
      Component: form
    },
    {
      path: RouteNames.edit,
      Component: form
    }
  ]
})

export const inventoryManagement: RouteObject[] = [
  inventoryManagementRoutes(
    RouteNames.controller,
    ControllerTypesList,
    ControllerTypeForm
  ),
  inventoryManagementRoutes(RouteNames.periphery, PeripheryTypesList, PeripheryTypeForm)
]

createControllersListener()
createPeripheryListener()
