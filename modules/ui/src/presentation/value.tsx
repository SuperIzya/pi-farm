import React from 'react'
import type { Selector } from '../types'
import type { MeasurementValue, Value as MValue, ValueWithError } from '../types/presentation'
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

export const SimpleValue = ({ value }: { value: MValue }) => <>{value}</>
export const WithError = ({ value, sigma }: ValueWithError) => (
  <>
    {value} ± {sigma}
  </>
)

const Display = <S extends RootState = RootState>({
  selector,
  unit
}: {
  selector: ValueSelector<S>
  unit: string
}) => {
  const value = useSelector(selector) ?? null
  if (value === null) return null
  const ValueTxt =
    typeof value === 'object' && 'value' in value ? (
      <WithError value={value.value} sigma={value.sigma} />
    ) : (
      <SimpleValue value={value} />
    )
  return (
    <div className={styles.value}>
      {ValueTxt} {unit}
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
