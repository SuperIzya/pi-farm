import type { RootState } from '../dashboard/types'
import { type Presentation, type PData, type PresentationName, type PresentationPairs, isGauge, isValue, isNoop } from '../types/presentation'
import { DefaultGaugeProps, Gauge, GaugePropsForm } from './gauge'
import { Value } from './value'
import type { MeasurementValue, GetProps } from '../types/presentation'
import { type Selector } from '../types'
import type { JSX } from 'react/jsx-dev-runtime'

export const Presentations: PresentationPairs = {
  Gauge: Gauge,
  Value: Value,
  Noop: () => () => null
}

type OneProp<K extends PresentationName> = {
  [k in K]: GetProps<k>
}

type DefaultPresentationProps = OneProp<PresentationName>

export const DefaultProps: DefaultPresentationProps = {
  Gauge: DefaultGaugeProps,
  Value: {unit: ''},
  Noop: undefined
}

type PropsFormProps<K extends PresentationName> = {
  onSave: (props: GetProps<K>) => void
  original?: GetProps<K>
}

export type PropsFormComponent<K extends PresentationName> = (props: PropsFormProps<K>) => JSX.Element | null

export type PropsForm<K extends PresentationName> = {
  [k in K]: PropsFormComponent<k>
}



type ShowProps<S extends RootState = RootState> = {
  definition?: Presentation 
  valueSelector: Selector<MeasurementValue, S>
}

export const Show = ({definition, valueSelector}: ShowProps) => {
  if(!definition || Object.keys(definition).length === 0) return null
  if(isGauge(definition)) {
    return Gauge(valueSelector)(definition.Gauge.props)
  }
  if(isValue(definition)) {
    return Value(valueSelector)(definition['Value'].props)
  }
  if(isNoop(definition)) {
    return null
  }
}