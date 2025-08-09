import type { ControllerType, PeripheryType } from '../types'

export type CommandName = 'save-periphery-type' | 'save-controller-type'

type CommandObj<T extends CommandName, D> = {
  [k in T]: { data: D }
}

type MaybeId<T> = Omit<T, 'id'> & { id?: number }

export type Command =
  | CommandObj<'save-periphery-type', MaybeId<PeripheryType>>
  | CommandObj<'save-controller-type', MaybeId<ControllerType>>
