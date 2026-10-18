import React from 'react'
import type { ClassName } from '../types'
import type { RootState } from '../store/root-store'
import * as styles from './gauge.scss'
import type { PresentationSelector } from '../types/presentation'
import { useSelector } from 'react-redux'
import type { PropsFormComponent } from '.'
import Input from '@mui/material/Input'
import IconButton from '@mui/material/IconButton'
import SaveIcon from '@mui/icons-material/Save'

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
  min: number
  max: number
  unit: string
  red?: RangeDef
  green?: RangeDef
  yellow?: RangeDef
}

export const DefaultGaugeProps: GaugeProps = {
  min: 0,
  max: 100,
  unit: '%'
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
  const value = useSelector(selector) || 0
  return (
    <div className={styles.hand} style={{ '--value': typeof value === 'number' ? value : 0 }}>
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

const RangeInput: React.FC<{
  range?: RangeDef
  label: string
  onChange: (range?: RangeDef) => void
}> = ({ range, onChange, label }) => {
  const min = range !== undefined && 'min' in range ? range.min : undefined
  const max = range !== undefined && 'max' in range ? range.max : undefined

  const submit = (min: number | undefined, max: number | undefined) => {
    if(max !== undefined && min !== undefined) {
      onChange({ min, max })
    } else if(max !== undefined) {
      onChange({ max })
    } else if(min !== undefined) {
      onChange({ min })
    } else {
      onChange(undefined)
    }
  }

  return (
    <>
      <span>{label}</span>
      <Input
        type="number"
        value={min ?? ''}
        onBlur={(e) => submit(e.target.value ? Number(e.target.value) : undefined, max)}
        placeholder="Min"
      />
      <Input
        type="number"
        value={max ?? ''}
        onBlur={(e) => submit(min, e.target.value ? Number(e.target.value) : undefined)}
        placeholder="Max"
      />
      <div></div>
    </>
  )
}

export const GaugePropsForm: PropsFormComponent<'Gauge'> = ({onSave, original}) => {
  const [min, setMin] = React.useState(original?.min ?? DefaultGaugeProps.min)
  const [max, setMax] = React.useState(original?.max ?? DefaultGaugeProps.max)
  const [unit, setUnit] = React.useState(original?.unit ?? DefaultGaugeProps.unit)
  const [red, setRed] = React.useState<RangeDef | undefined>(original?.red ?? DefaultGaugeProps.red)
  const [green, setGreen] = React.useState<RangeDef | undefined>(original?.green ?? DefaultGaugeProps.green)
  const [yellow, setYellow] = React.useState<RangeDef | undefined>(original?.yellow ?? DefaultGaugeProps.yellow)
  
  const save = () => onSave({ min, max, unit, red, green, yellow })

  return (
    <div className={styles.form}>
      <div>Range</div>
      <div>Min</div>
      <div>Max</div>
      <div>Unit</div>
      <span>Global</span>
      <Input
        type="number"
        value={min}
        onChange={(e) => setMin(Number(e.target.value))}
        placeholder="Min"
      />
      <Input
        type="number"
        value={max}
        onChange={(e) => setMax(Number(e.target.value))}
        placeholder="Max"
      />
      <Input
        type="text"
        value={unit}
        onChange={(e) => setUnit(e.target.value)}
        placeholder="Unit"
      />
        <RangeInput range={red} onChange={setRed} label="Red" />
        <RangeInput range={green} onChange={setGreen} label="Green" />
        <RangeInput range={yellow} onChange={setYellow} label="Yellow" />
      <IconButton onClick={save}><SaveIcon /></IconButton>
    </div>
  )
}