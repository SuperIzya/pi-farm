import React from 'react'
import { NavBar } from './utils/nav-bar'
import * as styles from './app.scss'
import { Outlet } from 'react-router'

export const Main = () => (
  <>
    <NavBar />
    <div className={styles.content}>
      <Outlet />
    </div>
  </>
)
