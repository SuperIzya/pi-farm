import { XYPosition } from '@xyflow/react'

export const flowDirections = ['in', 'out', 'both'] as const
export type FlowDirection = (typeof flowDirections)[number]

export type IdType = number

export const fieldTypes = ['String', 'Int', 'Boolean', 'Float', 'Double'] as const
export type FieldType = (typeof fieldTypes)[number]

export type PeripheryTypeId = IdType
export type ControllerTypeId = IdType
export type ControllerId = IdType
export type ConfigurationId = IdType

export type WithId<Id extends IdType> = { id: Id }

export type PeripheryConnection = {
  name: string
  direction: FlowDirection
  units: string
  type: FieldType
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
  typeId: ControllerTypeId
  name: string
  description: string
}

export type CtlAddress = {
  controllerId: ControllerId
  peripheryName: string
  peripheryConnectionName: string
  peripheryTypeName: string
}

export type ProcessorAddress = {
  name: string
  unit: string
  id: string
}

export type Connection = {
  name: string
  units: string
  type: FieldType
}

export type ProcessingUnit = {
  name: string
  description: string
  inbound: Connection[]
  outbound: Connection[]
  paramsSchema: Record<string, FieldType>
}

export type NewEntity<T> = Partial<T> & {
  canBeSaved: boolean
}

export type BaseState = {
  isLoading: boolean
  isInitialized: boolean
}

export type InventoryState<Id extends IdType, T extends WithId<Id>, NT = T> = BaseState & {
  knownEntities: T[]
  newEntity?: NewEntity<NT>
  editingIndex?: IdType
}

export type ToProcessor = {
  from: CtlAddress
  to: ProcessorAddress
  units: string
  type: FieldType
}

export type FromProcessor = {
  from: ProcessorAddress
  to: CtlAddress
  units: string
  type: FieldType
}

export type DataConnection = ToProcessor | FromProcessor


export type Address = {
  controllerId: ControllerId
  peripheryName: string
  peripheryConnectionName: string
  processorConnectionName: string
}
export type Processor = {
  unit: string
  graphId: string
  parameters: Record<string, unknown>
  inbound: Address[]
  outbound: Address[]
}

type Processors = Processor[]
type ProcessorNames<P extends Processors> = P[number]['unit']
type ControllerNames<P extends Processors> =
  | P[number]['inbound'][number]['controllerId']
  | P[number]['outbound'][number]['controllerId']

export type GraphData<P extends Processors> = {
  controllers: {
    [id in ControllerNames<P>]: {
      position: XYPosition
    }
  }
  processingUnits: {
    [id in ProcessorNames<P>]: {
      position: XYPosition
    }
  }
  svg?: string
}
export type Configuration<P extends Processors = Processors> = {
  id: ConfigurationId
  name: string
  description: string
  graphData: GraphData<P>
  processors: P
}

export type New<T> = Omit<T, 'id'>

export type MaybeId<T, Id extends IdType> = New<T> & { id?: Id }
