import React from 'react'
import { RouteObject } from 'react-router-dom'
import { composeRoutes, RouteNames } from '../utils/routes'
import { Loading } from '../utils/loading'

type Route = { Component: React.ComponentType }
type RouteResult = {
  List: React.ComponentType
  Form: React.ComponentType
}
type RoutePromise = Promise<RouteResult>

const convertPromise =
  (p: () => RoutePromise, extract: (r: RouteResult) => Route): (() => Promise<Route>) =>
  () =>
    p().then(result => extract(result))

const buildSectionRoute = (path: string, routePromise: () => RoutePromise): RouteObject => ({
  path,
  children: [
    {
      index: true,
      lazy: convertPromise(routePromise, ({ List }) => ({ Component: List })),
      HydrateFallback: Loading
    },
    {
      path: RouteNames.new,
      lazy: convertPromise(routePromise, ({ Form }) => ({ Component: Form })),
      HydrateFallback: Loading
    },
    {
      path: RouteNames.edit,
      lazy: convertPromise(routePromise, ({ Form }) => ({ Component: Form })),
      HydrateFallback: Loading
    }
  ]
})

export const inventoryRoutes: RouteObject[] = [
  {
    path: RouteNames.inventory,
    children: [
      buildSectionRoute(RouteNames.controller, () => import('./controller-types')),
      buildSectionRoute(RouteNames.periphery, () => import('./periphery-types'))
    ]
  },
  buildSectionRoute(
    composeRoutes(RouteNames.base, RouteNames.controller),
    () => import('./controller')
  ),
  buildSectionRoute(
    composeRoutes(RouteNames.base, RouteNames.configuration),
    () => import('./configuration')
  )
]
