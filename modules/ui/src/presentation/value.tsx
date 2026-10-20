import React from 'react'
import type { Selector } from '../types'
import type { MeasurementValue } from '../types/presentation'
import type { RootState } from '../store/root-store'
import { useSelector } from 'react-redux'
import * as styles from './value.scss'
import type { PropsFormComponent } from '.'
import Input from '@mui/material/Input'

type ValueSelector<S extends RootState = RootState> = Selector<MeasurementValue, S>

export type ValueProps = {
  unit: string
}

export const ValuePropsForm: PropsFormComponent<'Value'> = ({ onSave, original }) => (
  <Input
    type='text'
    value={original?.unit ?? ''}
    onBlur={e => onSave({ unit: e.target.value })}
    placeholder='Unit'
  />
)

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
