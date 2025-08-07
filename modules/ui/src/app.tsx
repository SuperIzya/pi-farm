import React from 'react'
import { inventoryManagement, InventoryManagement } from './inventory/management'
import * as styles from './app.scss'
import { NavBar } from './utils/nav-bar'
import { createBrowserRouter, RouterProvider, Outlet } from 'react-router'
import { RouteNames } from './utils/routes'
import { CommandsDispatcher } from './client'

const Index = () => (
  <>
    <NavBar />
    <div className={styles.content}>
      <Outlet />
    </div>
  </>
)

const router = createBrowserRouter([
  {
    path: RouteNames.base,
    children: [
      {
        index: true,
        Component: Index
      },
      {
        path: RouteNames.inventory,
        Component: Index,
        children: inventoryManagement
      }
    ]
  }
])

export const App = () => (
  <div className={styles.container}>
    <CommandsDispatcher>
      <InventoryManagement />
      <RouterProvider router={router} />
    </CommandsDispatcher>
  </div>
)
