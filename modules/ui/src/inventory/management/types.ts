export type NewType<T> = Partial<T> & {
  canBeSaved: boolean
}

export type InventoryState<T> = {
  knownTypes: T[]
  newType?: NewType<T>
  editingIndex?: number
}
