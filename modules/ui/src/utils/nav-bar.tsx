import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import * as styles from './nav-bar.scss'
import classNames from 'classnames'
import { composeRoutes, RouteNames } from './routes'

type NavLinkProps = {
  pathname: string
  text: string
}

const NavLink = ({ pathname, text }: NavLinkProps) => {
  const location = useLocation()
  return (
    <Link
      to={{ pathname }}
      className={classNames(styles.link, {
        [styles.disabled]: location.pathname.startsWith(pathname)
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
        pathname={composeRoutes(RouteNames.base, RouteNames.controller)}
        text={'Controllers'}
      />
      <NavLink
        pathname={composeRoutes(
          RouteNames.base,
          RouteNames.inventory,
          RouteNames.controller
        )}
        text={'Controller types'}
      />
      <NavLink
        pathname={composeRoutes(
          RouteNames.base,
          RouteNames.inventory,
          RouteNames.periphery
        )}
        text={'Periphery types'}
      />
    </nav>
  </div>
)
