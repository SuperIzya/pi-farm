export type NewType<T> = Partial<T> & {
  canBeSaved: boolean
}

export type IdType = number

export type WithId = { id: IdType }

export type InventoryState<T extends WithId> = {
  knownTypes: T[]
  newType?: NewType<T>
  editingIndex?: number
  isLoading: boolean
}
