import type { RootState } from '../store/root-store'
import type { ControllerId, Selector } from './'
import type { GaugeProps } from '../presentation/gauge'
import type { ValueProps } from '../presentation/value'

export const PresentationNames = ['Gauge', 'Value', 'Noop'] as const

export type PresentationName = (typeof PresentationNames)[number]

export type PresentationSelector<S extends RootState = RootState> = Selector<MeasurementValue, S>

type Component<P = void> = P extends void ? () => null : React.FC<P>

export type PresentationCreator<P = void> =
  <S extends RootState = RootState>(selector: PresentationSelector<S>) => Component<P>

export type PData<K extends PresentationName, P = void> = { 
  [k in K]: {
    creator?: PresentationCreator<P>
    props: P extends void ? {} : P
  }
}

export type Presentation =
  | PData<'Gauge', GaugeProps> 
  | PData<'Noop'>
  | PData<'Value', ValueProps>
  
type AsPresentation<K extends PresentationName> = PData<K, GetProps<K>> extends Presentation ? PData<K, GetProps<K>> : never
export const is = <K extends PresentationName>(key: K) => (presentation: Presentation): presentation is AsPresentation<K> =>
  key in presentation
export const isGauge = is('Gauge')

export const isValue = is('Value')

export const isNoop = is('Noop')

type GetAllProps<D extends Presentation> = D extends Presentation
  ? D extends PData<infer _K, infer P>
    ? P
    : never
  : never

type AllProps = GetAllProps<Presentation>
type IsVoid<K extends PresentationName> = PData<K> extends Presentation ? true : false

type FindProps<K extends PresentationName, P extends AllProps> =
  IsVoid<K> extends true
  ? P extends void
    ? P
    : never
  : P extends AllProps
    ? PData<K, P> extends Presentation
      ? P
      : never
    : never

export type GetProps<K extends PresentationName> = FindProps<K, AllProps>

export type PresentationOf<P extends PresentationName> = PresentationCreator<GetProps<P>>

type OnePair<K extends PresentationName> = {
  [k in K]: PresentationOf<k>
}

export type PresentationPairs = OnePair<PresentationName>

export type DefinitionOverride = {
  [key: string]: {
    [key: string]: Partial<Presentation>
  }
}

export type MeasurementValue = number | string | boolean | null

export type Values = {
  [peripheryName: string]: {
    [channelName: string]: MeasurementValue
  }
}

export type Measurement = {
  [key: ControllerId]: Values
}

export type MeasurementData = {
  'controller-id': number
  'data-points': Values
}