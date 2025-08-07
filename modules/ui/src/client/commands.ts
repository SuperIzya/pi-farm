import type { ControllerType, PeripheryType } from '../types'

export type Command =
  | ({
      type: 'save-periphery-type'
      id?: number
    } & Omit<PeripheryType, 'id'>)
  | ({
      type: 'save-controller-type'
      id?: number
    } & Omit<ControllerType, 'id'>)
