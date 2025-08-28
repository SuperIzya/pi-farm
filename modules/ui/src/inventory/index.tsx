import React, { useEffect } from 'react'
import { useOnReceiveData, useSendCommand } from '../client'
import { RouteObject } from 'react-router-dom'
import { composeRoutes, RouteNames } from '../utils/routes'

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
import {
  addNewEntity as addNewController,
  setEntities as setControllers
} from './controller/actions'
import { createListener as createControllersListener } from './controller-types/listener'
import { createListener as createPeripheryListener } from './periphery-types/listener'
import { useDispatch } from 'react-redux'
import { setLoading } from '../store/root-store'
import { ControllersList } from './controller/list'
import { ControllerForm } from './controller/form'

export const InventoryManagement = () => {
  const registerCallback = useOnReceiveData()
  registerCallback('periphery-type', addNewPeripheryType)
  registerCallback('controller-type', addNewControllerType)
  registerCallback('controller-types', setControllerTypes)
  registerCallback('periphery-types', setPeripheryTypes)
  registerCallback('controllers', setControllers)
  registerCallback('controller', addNewController)

  const sendCommand = useSendCommand()
  const dispatch = useDispatch()
  useEffect(() => {
    sendCommand('get-periphery-types')
    sendCommand('get-controller-types')
    sendCommand('get-controllers')
    sendCommand('get-configurations')
    dispatch(setLoading(true))
  }, [])
  return null
}

const buildSectionRoute = (
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

export const inventoryRoutes: RouteObject[] = [
  {
    path: RouteNames.inventory,
    children: [
      buildSectionRoute(RouteNames.controller, ControllerTypesList, ControllerTypeForm),
      buildSectionRoute(RouteNames.periphery, PeripheryTypesList, PeripheryTypeForm)
    ]
  },
  buildSectionRoute(
    composeRoutes(RouteNames.base, RouteNames.controller),
    ControllersList,
    ControllerForm
  )
]

createControllersListener()
createPeripheryListener()
