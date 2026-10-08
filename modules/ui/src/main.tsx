import React from 'react'
import { NavBar } from './utils/nav-bar'
import * as styles from './app.scss'
import { Outlet } from 'react-router'

export const Main = ({ children }: { children?: React.ReactNode }) => (
  <>
    <NavBar />
    <div className={styles.content}>{children || <Outlet />}</div>
  </>
)
