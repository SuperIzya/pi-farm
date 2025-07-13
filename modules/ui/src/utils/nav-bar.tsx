import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import styles from './nav-bar.scss'
import classNames from 'classnames'

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
    <NavLink path={'/inventory/controller-types'} text={'Controller types'} />
    <NavLink path={'/inventory/periphery-types'} text={'Periphery types'} />
  </div>
)
