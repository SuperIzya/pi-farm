import type { Edge, Node } from '@xyflow/react'
import type {
  BaseState,
  Configuration,
  ControllerId,
  CtlAddress,
  DataConnection,
  FieldType,
  IdType,
  NewEntity,
  FlowDirection,
  ProcessingUnit,
  ProcessorAddress,
  WithId
} from '../../types'
import type { RootState as ControllerState } from '../controller/types'
import type { WithItemKey } from '../../utils/list-mixin'

export type GraphEdge = Edge<DataConnection, 'default'>
export const nodeTypes = ['controller', 'processingUnit'] as const
export type NodeType = (typeof nodeTypes)[number]

type BaseEndpoint = {
  name: string
  units: string
  type: FieldType
  direction: FlowDirection
}

export type CtlEndpoint = BaseEndpoint & {
  controller: CtlAddress
}

export type ProcessorEndpoint = BaseEndpoint & {
  processor: ProcessorAddress
}

export type Endpoint = CtlEndpoint | ProcessorEndpoint

export type NodeData<T, EP extends Endpoint> = WithItemKey & {
  id: T
  endpoints: EP[]
}

export type ControllerData = NodeData<ControllerId, CtlEndpoint>
export type ProcessingUnitData = NodeData<string, ProcessorEndpoint> & {
  parameters: Record<string, unknown>
  unit: string
}

export type ProcessingNode = Node<ProcessingUnitData, 'processingUnit'>
export type ControllerNode = Node<ControllerData, 'controller'>

export type GraphNode = ProcessingNode | ControllerNode

type AllNodes<T extends GraphNode> = T extends GraphNode
  ? T extends Node<infer D, infer _T>
    ? D
    : never
  : never

type AllNodesData = AllNodes<GraphNode>
type FindNodeData<
  T extends NodeType,
  D extends AllNodesData & Record<string, unknown>
> = D extends AllNodesData ? (Node<D, T> extends GraphNode ? D : never) : never

export type ExtractNodeData<T extends NodeType> = FindNodeData<T, AllNodesData>

export type ProcessingUnits = Record<string, ProcessingUnit>

export type ConfigurationGraph = WithId<IdType> & {
  name: string
  description: string
  controllers: Record<ControllerId, ControllerNode>
  processingUnits: ProcessingNode[]
  edges: GraphEdge[]
}

export type ConfigurationsState = BaseState & {
  knownEntities: Configuration[]
  newEntity?: NewEntity<ConfigurationGraph>
  editingIndex?: IdType
}

export type ProcessingUnitsState = {
  isInitialized: boolean
  isLoading: boolean
  entities: ProcessingUnits
}

export type RootState = {
  configurations: ConfigurationsState
  processingUnits: ProcessingUnitsState
} & ControllerState
