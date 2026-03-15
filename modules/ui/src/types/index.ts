import type { Edge, Node } from '@xyflow/react'

export type PeripheryDirection = 'in' | 'out' | 'both'

export type IdType = number

export type WithId = { id: IdType }

export type PeripheryType = WithId & {
  name: string
  description: string
  image: string
  direction: PeripheryDirection
  units: string
}

export type Peripheries = Record<string, IdType>

export type ControllerType = WithId & {
  name: string
  description: string
  schema: string
  code: string
  peripheries: Peripheries
}

export type Controller = WithId & {
  typeId: IdType
  name: string
  description: string
}

export type ConfigurationEndpoint = Record<IdType, IdType[]>

export type Configuration = WithId & {
  name: string
  description: string
  inputs: ConfigurationEndpoint
  outputs: ConfigurationEndpoint
  nodes: Node[]
  edges: Edge[]
  preview: string
}

export type NewEntity<T> = Partial<T> & {
  canBeSaved: boolean
}

export type InventoryState<T extends WithId> = {
  knownEntities: T[]
  newEntity?: NewEntity<T>
  editingIndex?: IdType
  isLoading: boolean
  isInitialized: boolean
}

export type NoId<T> = Omit<T, 'id'>

export type MaybeId<T> = NoId<T> & { id?: IdType }
