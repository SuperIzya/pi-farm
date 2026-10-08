import React from 'react'
import { Dashboard } from './dashboard'
import { createBrowserRouter, RouterProvider } from 'react-router'
import { RouteNames } from './utils/routes'
import { CommandsDispatcher } from './client'
import { inventoryRoutes } from './inventory'
import { Main } from './main'
import { Error } from './utils/error'
import * as styles from './app.scss'

const router = createBrowserRouter([
  {
    path: RouteNames.base,
    children: [
      {
        index: true,
        Component: Main
      },
      {
        path: RouteNames.dashboard,
        element: (
          <Main>
            <Dashboard />
          </Main>
        )
      },
      {
        Component: Main,
        children: inventoryRoutes
      }
    ]
  }
])

export const App = () => (
  <div className={styles.container}>
    <CommandsDispatcher>
      <Error />
      <RouterProvider router={router} />
    </CommandsDispatcher>
  </div>
)
