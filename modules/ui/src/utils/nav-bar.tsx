import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import * as styles from './nav-bar.scss'
import classNames from 'classnames'
import { composeRoutes, RouteNames } from './routes'

interface NavLinkProps {
  path: string
  text: string
}

const NavLink = ({ path, text }: NavLinkProps) => {
  const location = useLocation()
  return (
    <Link
      to={path}
      className={classNames(styles.link, {
        [styles.disabled]: location.pathname.endsWith(path)
      })}
    >
      {text}
    </Link>
  )
}

export const NavBar = () => (
  <div className={styles.container}>
    <nav>
      <NavLink
        path={composeRoutes(RouteNames.base, RouteNames.inventory, RouteNames.controller)}
        text={'Controller types'}
      />
      <NavLink
        path={composeRoutes(RouteNames.base, RouteNames.inventory, RouteNames.periphery)}
        text={'Periphery types'}
      />
    </nav>
  </div>
)
