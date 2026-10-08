import React from 'react'
import type { ClassName } from '../types'
import type { RootState } from '../store/root-store'
import styles from './gauge.scss'
import type { PresentationSelector } from '../types/presentation'
import { useSelector } from 'react-redux'

type RangeDef =
  | {
      min: number
    }
  | {
      min: number
      max: number
    }
  | {
      max: number
    }

export type GaugeProps = {
  type?: 'gauge'
  min: number
  max: number
  unit: string
  red?: RangeDef
  green?: RangeDef
  yellow?: RangeDef
}

const Range: React.FC<{ range?: RangeDef } & ClassName> = ({ range, className }) => {
  if (range) {
    if ('min' in range && 'max' in range) {
      return (
        <div
          className={className}
          style={{ ['--range-min']: range.min, ['--range-max']: range.max }}
        />
      )
    }
    if ('min' in range) {
      return <div className={className} style={{ ['--range-min']: range.min }} />
    }
    if ('max' in range) {
      return <div className={className} style={{ ['--range-max']: range.max }} />
    }
  }
  return null
}

type HandProps<S extends RootState = RootState> = {
  selector: PresentationSelector<S>
  unit: string
}

const Hand = <S extends RootState = RootState>({ selector, unit }: HandProps<S>) => {
  const value = useSelector(selector) || ''
  return (
    <div className={styles.hand} style={{ ['--value']: value }}>
      <div className={styles.value}>
        {value} {unit}
      </div>
      <div className={styles.pointer} />
    </div>
  )
}

export const Gauge =
  <S extends RootState = RootState>(selector: PresentationSelector<S>) =>
  ({ min, max, red, green, yellow, unit }: GaugeProps) => (
    <div className={styles.container} style={{ ['--gauge-min']: min, ['--gauge-max']: max }}>
      <Range range={red} className={styles.redRange} />
      <Range range={green} className={styles.greenRange} />
      <Range range={yellow} className={styles.yellowRange} />
      <Hand selector={selector} unit={unit} />
    </div>
  )
