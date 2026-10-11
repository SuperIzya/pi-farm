import type { ControllerId } from '../types'
import type { Definition, Values } from '../types/presentation'
import type { RootState as BaseRootState } from '../inventory/configuration/types'
import type { Measurement } from '../types/presentation'



export type Presentation = {
  [peripheryName: string]: {
    [channelName: string]: Definition
  }
}

export type State = Measurement & {
  isInitialized: boolean
}

export type RootState = { dashboard: State } & BaseRootState