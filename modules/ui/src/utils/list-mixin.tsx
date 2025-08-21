import React, { CSSProperties } from 'react'
import { connect } from 'react-redux'
import classNames from 'classnames'

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
}

const defaultCss: CssVars = {
  columns: 'auto-fill',
  columnMax: '1fr',
  columnMin: '200px',
  overflow: 'auto',
  gridGap: '1rem',
  maxHeight: '100%',
  maxWidth: '100%'
}

type ListOuterProps = {
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

type ListProps<T extends object = Empty> = T & { count: number }

const listElement =
  <T extends object>(Item: ListItem<T>) =>
  (props: ListProps<T>) => {
    const { count, ...restArgs } = props
    return (
      <>
        {Array.from(Array(count).keys()).map((key) => (
          <Item {...(restArgs as T)} itemKey={key} key={key} />
        ))}
      </>
    )
  }

export type CountSelector<S, Q, P> = (state: S, props: P) => Q[]
type CountProp = { count: number }

const mapStateToProps =
  <S, Q, P>(selector: CountSelector<S, Q, P>) =>
  (state: S, props: P): CountProp => ({
    count: selector(state, props).length
  })

const connectState = <S, Q, P>(selector: CountSelector<S, Q, P>) =>
  connect(mapStateToProps(selector))

export const createList =
  <S, Q, P>(selector: CountSelector<S, Q, P>) =>
  <T extends object>(item: ListItem<T>) =>
  (props: T & P & ListOuterProps) => {
    const { listConfigCss, containerClassName, ...rawProps } = props
    const ownProps = rawProps as T & P
    const List = connectState(selector)(({ count }: CountProp) =>
      listElement(item)({ ...ownProps, count })
    )
    return (
      <div
        className={classNames(containerClassName)}
        style={computeStyle({ ...defaultCss, ...(listConfigCss || {}) })}
      >
        <List {...ownProps} />
      </div>
    )
  }
