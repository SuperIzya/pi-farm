import type { ControllerId } from '../types'

export type State = {
  isInitialized: boolean
  [controllerId: ControllerId]: {
    [peripheryName: string]: {
      [channelName: string]: {
        value: number | string
      }
    }
  }
}
