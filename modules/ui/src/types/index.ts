export type PeripheryDirection = 'in' | 'out' | 'both'

export type IdType = number

export type PeripheryTypeId = IdType
export type ControllerTypeId = IdType
export type ControllerId = IdType
export type ConfigurationId = IdType

export type WithId<Id extends IdType> = { id: Id }

export type PeripheryConnection = {
  name: string
  direction: PeripheryDirection
  units: string
  type: string
}

export type PeripheryType = WithId<PeripheryTypeId> & {
  name: string
  description: string
  image: string
  connections: PeripheryConnection[]
}

export type Peripheries = Record<string, PeripheryTypeId>

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

export type CtlAddress = {
  controllerId: IdType
  peripheryId: string
  name: string
}

export type ProcessorAddress = {
  name: string
  processingUnitId: string
}

export type ConnectionType = 'in' | 'out'
export type Connection<D extends ConnectionType> = {
  name: string
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

export type BaseState = {
  isLoading: boolean
  isInitialized: boolean
}

export type InventoryState<Id extends IdType, T extends WithId<Id>> = BaseState & {
  knownEntities: T[]
  newEntity?: NewEntity<T>
  editingIndex?: IdType
}

export type DataConnection =
  | {
      from: CtlAddress
      to: ProcessorAddress
      units: string
      type: string
    }
  | {
      from: ProcessorAddress
      to: CtlAddress
      units: string
      type: string
    }
export type Configuration = {
  id: ConfigurationId
  name: string
  description: string
  processingUnits: string[]
  connections: DataConnection[]
}

export type New<T> = Omit<T, 'id'>

export type MaybeId<T, Id extends IdType> = New<T> & { id?: Id }
