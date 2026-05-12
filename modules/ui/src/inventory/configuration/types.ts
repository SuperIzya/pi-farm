import type { Edge, Node } from '@xyflow/react'
import type {
  BaseState,
  ControllerId,
  DataConnection,
  IdType,
  NewEntity,
  ProcessingUnit,
  WithId
} from '../../types'
import type { RootState as ControllerState } from '../controller/types'
import type { WithItemKey } from '../../utils/list-mixin'
import type { Endpoint } from './graph/selectors'

export type GraphEdge = Edge<DataConnection, 'default'>

export type NodeData<T> = WithItemKey & {
  id: T
  endpoints: Endpoint[]
}

export type ControllerData = NodeData<ControllerId>
export type ProcessingUnitData = NodeData<string>

export type ProcessingNode = Node<ProcessingUnitData, 'processingUnit'>
export type ControllerNode = Node<ControllerData, 'controller'>

export type GraphNode = ProcessingNode | ControllerNode

export type ConfigurationGraph = WithId<IdType> & {
  name: string
  description: string
  controllers: {
    [controllerId: ControllerId]: ControllerNode
  }
  processingUnits: {
    [processingUnitId: string]: ProcessingNode
  }
  edges: GraphEdge[]
}

export type ConfigurationsState = BaseState & {
  knownEntities: ConfigurationGraph[]
  newEntity?: NewEntity<ConfigurationGraph>
  editingIndex?: IdType
}

export type ProcessingUnitsState = {
  isInitialized: boolean
  isLoading: boolean
  entities: {
    [name: string]: ProcessingUnit
  }
}

export type RootState = {
  configurations: ConfigurationsState
  processingUnits: ProcessingUnitsState
} & ControllerState
