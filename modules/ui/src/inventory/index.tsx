import React from 'react'
import { RouteObject } from 'react-router-dom'
import { RouteNames } from '../utils/routes'
import { List } from './controller/list'
import { Form } from './controller/form'

const controllersRoutes = (): RouteObject[] => [
  {
    path: RouteNames.controller,
    children: [
      {
        index: true,
        Component: List
      },
      {
        path: RouteNames.new,
        Component: Form
      },
      {
        path: RouteNames.edit,
        Component: Form
      }
    ]
  }
]

export const inventoryRoutes: RouteObject[] = [...controllersRoutes()]
