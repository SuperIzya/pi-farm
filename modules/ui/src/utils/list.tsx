import React from 'react'
import { connect } from 'react-redux'

type Empty = { [key: string]: never }

export type ItemProps<T extends object = Empty> = Empty extends T
  ? { key: number }
  : Omit<T, 'key'> & { key: number }

export type ListItem<T extends object = Empty> = (props: ItemProps<T>) => React.ReactNode

type ListProps<T extends object = Empty> = Empty extends T
  ? { count: number }
  : T & { count: number }

const listElement =
  <T extends object = Empty>(item: ListItem<T>) =>
  (props: ListProps<T>) => (
    <>
      {Array.from(Array(props.count).keys()).map((key) => {
        const { count: _, ...restArgs } = props
        return item({ ...(restArgs as T), key })
      })}
    </>
  )

export type CountSelector<S, Q, P> = (state: S, props: P) => Q[]

const mapStateToProps =
  <S, Q, P>(selector: CountSelector<S, Q, P>) =>
  (state: S, props: P) => ({
    count: selector(state, props).length
  })

export const createList =
  <S, Q, P>(selector: CountSelector<S, Q, P>) =>
  <T extends object = Empty>(item: ListItem<T>) =>
  (ownProps: T & P) => {
    const List = connect(mapStateToProps(selector))(({ count }: { count: number }) =>
      listElement(item)({
        ...ownProps,
        count
      })
    )
    return <List {...ownProps} />
  }
