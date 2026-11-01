import type { Presentation } from '../types/presentation'
import type { RootState as BaseRootState } from '../inventory/configuration/types'
import type { Measurement } from '../types/presentation'

export type ControllerPresentation = {
  [peripheryName: string]: {
    [channelName: string]: Presentation
  }
}

export type State = {
  isInitialized: boolean
  data?: Measurement
}

export type RootState = { dashboard: State } & BaseRootState
