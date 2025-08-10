import React from 'react'
import { useOnReceiveData, useSendCommand } from '../../client'
import { RouteObject } from 'react-router-dom'
import { RouteNames } from '../../utils/routes'

import { PeripheryTypesList } from './periphery-types/list'
import { PeripheryTypeForm } from './periphery-types/form'
import { ControllerTypesList } from './controller-types/list'
import { ControllerTypeForm } from './controller-types/form'
import { addNewType as addNewControllerType } from './controller-types/actions'
import { addNewType as addNewPeripheryType } from './periphery-types/actions'
import { createListener as createControllersListener } from './controller-types/listener'
import { createListener as createPeripheryListener } from './periphery-types/listener'

export const InventoryManagement = () => {
  const [render, setRender] = React.useState(false)
  const registerCallback = useOnReceiveData()
  registerCallback('periphery-type', (data) => addNewPeripheryType(data))
  registerCallback('controller-type', (data) => addNewControllerType(data))

  const sendCommand = useSendCommand()
  React.useEffect(() => {
    if (!render) {
      sendCommand('getPeripheryTypes')
      sendCommand('getControllerTypes')
      setRender(true)
    }
  }, [])
  return null
}

const inventoryRoutes = (
  path: string,
  list: React.ComponentType,
  form: React.ComponentType
): RouteObject[] => [
  {
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
  }
]

export const inventoryManagement: RouteObject[] = [
  ...inventoryRoutes(RouteNames.controller, ControllerTypesList, ControllerTypeForm),
  ...inventoryRoutes(RouteNames.periphery, PeripheryTypesList, PeripheryTypeForm)
]

createControllersListener()
createPeripheryListener()
