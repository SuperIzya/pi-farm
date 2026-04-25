import React, { CSSProperties } from 'react'
import classNames from 'classnames'
import type { RootState } from '../inventory/controller-types/types'

type CssVars = {
  columns?: number | string
  columnMin?: number | string
  columnMax?: number | string
  gridGap?: number | string
  padding?: number | string
  margin?: number | string
  width?: number | string
  height?: number | string
  maxHeight?: number | string
  maxWidth?: number | string
  minHeight?: number | string
  minWidth?: number | string
  overflow?: 'auto' | 'hidden' | 'scroll' | 'visible'
  itemMaxHeight?: number | string
}

const defaultCss: CssVars = {
  columns: 2,
  columnMax: '1fr',
  columnMin: '250px',
  overflow: 'auto',
  gridGap: '5px',
  maxHeight: '100%',
  maxWidth: '100%',
  itemMaxHeight: '300px'
}

export type ListOuterProps = {
  listConfigCss?: CssVars
  containerClassName?: string
}

const computeStyle = (vars: CssVars): CSSProperties => {
  const addKey = <K extends keyof CssVars>(key: K, value: CssVars[K]) => ({
    [`--${key}`]: value
  })
  return (Object.keys(vars) as (keyof CssVars)[]).reduce(
    (acc, key) => (vars[key] !== undefined ? { ...acc, ...addKey(key, vars[key]) } : acc),
    {}
  )
}
// @typescript-eslint/no-empty-object-type
type Empty = {}

export type WithItemKey = { itemKey: number }
export type ItemProps<T extends object = Empty> = Omit<T, 'itemKey'> & WithItemKey

export type ListItem<T extends object = Empty> = (props: ItemProps<T>) => React.ReactNode

export const getListKey = (state: RootState, { itemKey }: ItemProps) => itemKey

export type GenericListProps<T extends object = Empty> = T & {
  count: number
  Item: ListItem<T>
} & ListOuterProps

export const GenericList = <T extends object>(props: GenericListProps<T>) => {
  const { Item, count, listConfigCss, containerClassName, ...restArgs } = props
  return (
    <div
      className={classNames(containerClassName)}
      style={computeStyle({ ...defaultCss, ...(listConfigCss || {}) })}
    >
      {Array.from(Array(count).keys()).map((key) => (
        <Item {...(restArgs as T)} itemKey={key} key={key} />
      ))}
    </div>
  )
}
