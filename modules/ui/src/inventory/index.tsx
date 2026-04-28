import React from 'react'
import { RouteObject } from 'react-router-dom'
import { composeRoutes, RouteNames } from '../utils/routes'

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

const buildSectionRoute = (
  path: string,
  list: () => RoutePromise,
  form: () => RoutePromise
): RouteObject => ({
  path,
  children: [
    {
      index: true,
      lazy: convertPromise(list, ({ List }) => ({ Component: List }))
    },
    {
      path: RouteNames.new,
      lazy: convertPromise(form, ({ Form }) => ({ Component: Form }))
    },
    {
      path: RouteNames.edit,
      lazy: convertPromise(form, ({ Form }) => ({ Component: Form }))
    }
  ]
})

export const inventoryRoutes: RouteObject[] = [
  {
    path: RouteNames.inventory,
    children: [
      buildSectionRoute(
        RouteNames.controller,
        () => import('./controller-types'),
        () => import('./controller-types')
      ),
      buildSectionRoute(
        RouteNames.periphery,
        () => import('./periphery-types'),
        () => import('./periphery-types')
      )
    ]
  },
  buildSectionRoute(
    composeRoutes(RouteNames.base, RouteNames.controller),
    () => import('./controller'),
    () => import('./controller')
  ),
  buildSectionRoute(
    composeRoutes(RouteNames.base, RouteNames.configuration),
    () => import('./configuration'),
    () => import('./configuration')
  )
]
