import React, { CSSProperties } from 'react'
import classNames from 'classnames'
import { RootState } from '../store/root-store'
import { Selector, WithSelector } from '../types'

export type CssVars = {
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

export const defaultCss: CssVars = {
  columns: 3,
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
const asKey = <K extends keyof CssVars>(key: K, value: CssVars[K]) => ({
  [`--${key}`]: value
})
const computeStyle = (vars: CssVars): CSSProperties =>
  (Object.keys(vars) as (keyof CssVars)[]).reduce(
    (acc, key) => (vars[key] !== undefined ? { ...acc, ...asKey(key, vars[key]) } : acc),
    {}
  )

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
type Empty = {}

export type WithKey = { itemKey: number }

export type ItemProps<
  I extends object = Empty,
  S extends RootState = RootState,
  T extends object = Empty
> = Omit<T, 'selector'> & WithSelector<I, S>

export type ListItem<
  I extends object = Empty,
  S extends RootState = RootState,
  T extends object = Empty
> = (props: ItemProps<I, S, T>) => React.ReactNode

export type GenericListProps<
  I extends object = Empty,
  S extends RootState = RootState,
  T extends object = Empty
> = {
  count: number
  Item: ListItem<I, S, T & WithKey>
  selectorFactory: (key: number) => Selector<I, S>
} & Omit<T, 'count' | 'Item' | 'selector'>
  & ListOuterProps

export const GenericList = <
  I extends object = Empty,
  S extends RootState = RootState,
  T extends object = Empty
>(
  props: GenericListProps<I, S, T>
) => {
  const {
    Item,
    count,
    listConfigCss,
    containerClassName,
    selectorFactory: selector,
    ...restArgs
  } = props
  return (
    <div
      className={classNames(containerClassName)}
      style={computeStyle({ ...defaultCss, ...(listConfigCss || {}) })}
    >
      {Array.from(Array(count).keys()).map(key => (
        <Item {...(restArgs as T)} selector={selector(key)} key={key} itemKey={key} />
      ))}
    </div>
  )
}
