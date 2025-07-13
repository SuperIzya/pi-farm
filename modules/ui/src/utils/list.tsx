import React from 'react'
import { connect } from 'react-redux'

export type ItemArg = { index: number }

type ListProps = {
  count: number
  item: (arg: ItemArg) => React.ReactNode
}

const ListElement = (args: ListProps) => (
  <>{Array.from(Array(args.count).keys()).map((i) => args.item({ index: i }))}</>
)

export type CountSelector<T, Q> = (state: T) => Q[]

const mapStateToProps =
  <T, Q>(selector: CountSelector<T, Q>) =>
  (state: T) => ({
    count: selector(state).length
  })

export const createList = <T, Q>(selector: CountSelector<T, Q>) =>
  connect(mapStateToProps(selector))(ListElement)
