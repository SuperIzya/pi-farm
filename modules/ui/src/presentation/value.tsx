import React from 'react'
import type { Selector } from '../types'
import type { RootState } from '../store/root-store'
import { useSelector } from 'react-redux'
import styles from './value.scss'

type ValueSelector<S extends RootState = RootState> = Selector<number | string, S>

export type ValueProps = {
  type?: 'value'
  unit: string
}

const Display = <S extends RootState = RootState>({
  selector,
  unit
}: {
  selector: ValueSelector<S>
  unit: string
}) => {
  const value = useSelector(selector)
  return (
    <div className={styles.value}>
      {value} {unit}
    </div>
  )
}

export const Value =
  <S extends RootState = RootState>(selector: ValueSelector<S>) =>
  ({ unit }: ValueProps) => (
    <div className={styles.container}>
      <Display selector={selector} unit={unit} />
    </div>
  )
