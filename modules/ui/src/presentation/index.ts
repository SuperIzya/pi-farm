import type { PresentationPair } from '../types/presentation'
import { Gauge } from './gauge'
import { Value } from './value'

export const Presentations: PresentationPair = {
  Gauge: Gauge,
  Value: Value
}
