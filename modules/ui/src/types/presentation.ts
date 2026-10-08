import type { RootState } from '../store/root-store'
import type { Selector } from './'
import type { GaugeProps } from '../presentation/gauge'
import type { ValueProps } from '../presentation/value'

export const PresentationNames = ['Gauge', 'Value'] as const

export type Presentation = (typeof PresentationNames)[number]

export type PresentationSelector<S extends RootState = RootState> = Selector<number | string, S>

export type PresentationCreator<P> = <S extends RootState = RootState>(
  selector: PresentationSelector<S>
) => React.FC<P>

type PData<K extends Presentation, P> = { [k in K]: PresentationCreator<P> }
type AllPairs = PData<'Gauge', GaugeProps> | PData<'Value', ValueProps>

type GetAllProps<D extends AllPairs> = D extends AllPairs
  ? D extends PData<infer _K, infer P>
    ? P
    : never
  : never

type AllProps = GetAllProps<AllPairs>
type FindProps<K extends Presentation, P extends AllProps> = P extends AllProps
  ? PData<K, P> extends AllPairs
    ? P
    : never
  : never

type GetProps<K extends Presentation> = FindProps<K, AllProps>

export type PresentationPair = {
  [key in Presentation]: PresentationCreator<GetProps<key>>
}

export type PresentationDefinition<P extends Presentation> = P extends Presentation
  ? {
      type?: P
      props: GetProps<P>
    }
  : never

export type Definition = PresentationDefinition<Presentation>

export type DefinitionOverride = {
  [key: string]: {
    [key: string]: Partial<Definition>
  }
}
