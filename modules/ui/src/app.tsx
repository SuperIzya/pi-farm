import React, { ReactNode } from 'react'
import { Route, BrowserRouter, Routes } from 'react-router'
import {
  PeripheryTypesList,
  PeripheryTypeForm
} from './inventory/management/periphery-types'
import {
  ControllerTypeForm,
  ControllerTypesList
} from './inventory/management/controller-types'
import styles from './app.scss'
import { NavBar } from './utils/nav-bar'

const Empty = () => <></>
type InventoryProps = {
  path: string
  Index: () => ReactNode
  Form: () => ReactNode
}
const Inventory = ({ path, Index, Form }: InventoryProps) => (
  <Route path={path}>
    <Route index element={<Index />} />
    <Route path={'new'} element={<Form />} />
    <Route path={'edit/:id'} element={<Form />} />
  </Route>
)

export const App = () => (
  <div className={styles.container}>
    <NavBar />
    <BrowserRouter basename={'/'}>
      <Routes>
        <Route index element={<Empty />} />
        <Route path={'inventory'}>
          <Inventory
            path={'controller-types'}
            Index={ControllerTypesList}
            Form={ControllerTypeForm}
          />
          <Inventory
            path={'periphery-types'}
            Index={PeripheryTypesList}
            Form={PeripheryTypeForm}
          />
        </Route>
      </Routes>
    </BrowserRouter>
  </div>
)
