import type { RootState } from '../dashboard/types'
import type { Presentation, PData, PresentationName, PresentationPairs } from '../types/presentation'
import { Gauge } from './gauge'
import { Value } from './value'
import type { MeasurementValue } from '../types/presentation'
import { type Selector } from '../types'
import type { GetProps } from 'react-redux'

export const Presentations: PresentationPairs = {
  Gauge: Gauge,
  Value: Value,
  Noop: () => () => null
}

type ShowProps<S extends RootState = RootState> = {
  definition?: Presentation 
  valueSelector: Selector<MeasurementValue, S>
}

export const Show = ({definition, valueSelector}: ShowProps) => {
  if(!definition || Object.keys(definition).length === 0) return null
  const key = Object.keys(definition)[0] as PresentationName
  type K = typeof key
  const pDef = definition as PData<K, GetProps<K>>
  const creator = Presentations[key]
  if(!('props' in pDef[key])) return null
  return creator(valueSelector)(pDef[key].props)
}