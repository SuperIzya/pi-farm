export type PeripheryDirection = 'in' | 'out' | 'both'

export type IdType = number

export type PeripheryTypeId = IdType
export type ControllerTypeId = IdType
export type ControllerId = IdType
export type ConfigurationId = IdType

export type WithId<Id extends IdType> = { id: Id }

export type PeripheryType = WithId<PeripheryTypeId> & {
  name: string
  description: string
  image: string
  direction: PeripheryDirection
  units: string
  type: string
}

export type Peripheries = Record<string, IdType>

export type ControllerType = WithId<ControllerTypeId> & {
  name: string
  description: string
  schema: string
  code: string
  peripheries: Peripheries
}

export type Controller = WithId<ControllerId> & {
  typeId: IdType
  name: string
  description: string
}

export type ConfigurationEndpoint = Record<string, IdType[]>

export type BaseConfiguration = WithId<ConfigurationId> & {
  name: string
  description: string
  processingUnit: string
  inbound: Address[]
  outbound: Address[]
  additional: Record<string, unknown>
}

export type Address = {
  controllerId: IdType
  peripheryId: string
  name: string
}

export type Configuration = BaseConfiguration & {
  inputs: ConfigurationEndpoint
  outputs: ConfigurationEndpoint
  nodes: unknown[]
  edges: unknown[]
  preview: string
}
export type ConnectionType = 'in' | 'out'
export type Connection<D extends ConnectionType> = {
  units: string
  type: string
  direction: D
}

export type ProcessingUnit = {
  name: string
  description: string
  inbound: Connection<'in'>[]
  outbound: Connection<'out'>[]
  params: Record<string, unknown>
}

export type NewEntity<T> = Partial<T> & {
  canBeSaved: boolean
}

export type InventoryState<Id extends IdType, T extends WithId<Id>> = {
  knownEntities: T[]
  newEntity?: NewEntity<T>
  editingIndex?: IdType
  isLoading: boolean
  isInitialized: boolean
}

export type New<T> = Omit<T, 'id'>

export type MaybeId<T, Id extends IdType> = New<T> & { id?: Id }
