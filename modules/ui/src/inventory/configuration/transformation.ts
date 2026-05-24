import type {
  Configuration,
  Controller,
  ControllerId,
  ControllerType,
  ControllerTypeId,
  CtlAddress,
  FieldType,
  IdType,
  PeripheryType,
  PeripheryTypeId,
  ProcessingUnit,
  ProcessorAddress
} from '../../types'
import type {
  ConfigurationGraph,
  ControllerNode,
  CtlEndpoint,
  Endpoint,
  GraphEdge,
  ProcessingNode
} from './types'

type ControllerMap = Record<ControllerId, ControllerNode>

type GraphAccumulator = {
  controllers: ControllerMap
  processingUnits: ProcessingNode[]
  edges: GraphEdge[]
}

type BindingEntry = {
  addr: {
    controllerId: IdType
    peripheryId: string
    name: string
  }
  conn: {
    name: string
    units: string
    type: FieldType
  }
  isInbound: boolean
}

type ProcessedBinding = {
  ctls: ControllerMap
  edgeList: GraphEdge[]
}

type Lookup = {
  controllers: Record<ControllerId, Controller>
  controllerTypes: Record<ControllerTypeId, ControllerType>
  peripheryTypes: Record<PeripheryTypeId, PeripheryType>
  units: Record<string, ProcessingUnit>
}

const buildControllerEndpoints = (controllerId: ControllerId, lookup: Lookup): CtlEndpoint[] => {
  const controller = lookup.controllers[controllerId]
  if (!controller) return []

  const ctlType = lookup.controllerTypes[controller.typeId]
  if (!ctlType) return []

  return Object.entries(ctlType.peripheries).flatMap(([peripheryId, peripheryTypeId]) => {
    const peripheryType = lookup.peripheryTypes[peripheryTypeId]
    if (!peripheryType) return []

    return peripheryType.connections.map(conn => ({
      name: conn.name,
      units: conn.units,
      type: conn.type,
      direction: conn.direction,
      controller: { controllerId, peripheryId, name: conn.name } as CtlAddress
    }))
  })
}

const getOrCreateController = (
  ctls: ControllerMap,
  controllerId: ControllerId,
  config: Configuration,
  lookup: Lookup
): ControllerMap => {
  if (ctls[controllerId]) return ctls

  return {
    ...ctls,
    [controllerId]: {
      id: controllerId.toString(),
      type: 'controller' as const,
      position: config.graphData.controllers[controllerId]?.position ?? { x: 0, y: 0 },
      data: {
        id: controllerId,
        itemKey: controllerId,
        endpoints: buildControllerEndpoints(controllerId, lookup)
      }
    }
  }
}

const buildProcessorNode = (
  config: Configuration,
  processor: Configuration['processors'][number],
  unit: ProcessingUnit,
  idx: number
): ProcessingNode => ({
  id: processor.graphId,
  type: 'processingUnit',
  position: config.graphData.processingUnits[processor.graphId]?.position ?? { x: 0, y: 0 },
  data: {
    id: processor.unit,
    unit: processor.unit,
    itemKey: idx,
    parameters: processor.parameters,
    endpoints: [
      ...unit.inbound.map(c => ({
        name: c.name,
        units: c.units,
        type: c.type,
        direction: 'in' as const,
        processor: { name: c.name, unit: processor.unit, id: processor.graphId }
      })),
      ...unit.outbound.map(c => ({
        name: c.name,
        units: c.units,
        type: c.type,
        direction: 'out' as const,
        processor: { name: c.name, unit: processor.unit, id: processor.graphId }
      }))
    ]
  }
})

const collectBindings = (
  processor: Configuration['processors'][number],
  unit: ProcessingUnit
): BindingEntry[] => [
  ...processor.inbound.reduce((acc, addr, i) => {
    const conn = unit.inbound[i]
    return conn ? [...acc, { addr, conn, isInbound: true }] : acc
  }, [] as BindingEntry[]),
  ...processor.outbound.reduce((acc, addr, i) => {
    const conn = unit.outbound[i]
    return conn ? [...acc, { addr, conn, isInbound: false }] : acc
  }, [] as BindingEntry[])
]

const buildEdge = (
  processorUnit: string,
  ctlAddress: CtlAddress,
  procAddress: ProcessorAddress,
  conn: BindingEntry['conn'],
  isInbound: boolean
): GraphEdge => ({
  id: isInbound
    ? `e-${ctlAddress.controllerId}-${ctlAddress.peripheryId}-${ctlAddress.name}-${processorUnit}-${conn.name}`
    : `e-${processorUnit}-${conn.name}-${ctlAddress.controllerId}-${ctlAddress.peripheryId}-${ctlAddress.name}`,
  source: isInbound ? `${ctlAddress.controllerId}` : processorUnit,
  target: isInbound ? processorUnit : `${ctlAddress.controllerId}`,
  type: 'default',
  data: isInbound
    ? { from: ctlAddress, to: procAddress, units: conn.units, type: conn.type }
    : { from: procAddress, to: ctlAddress, units: conn.units, type: conn.type }
})

const processBindings = (
  config: Configuration,
  processorUnit: string,
  bindings: BindingEntry[],
  initialCtls: ControllerMap,
  lookup: Lookup
): ProcessedBinding =>
  bindings.reduce(
    ({ ctls, edgeList }, { addr, conn, isInbound }) => {
      const ctlAddress: CtlAddress = {
        controllerId: addr.controllerId,
        peripheryId: addr.peripheryId,
        name: addr.name
      }
      const procAddress: ProcessorAddress = {
        name: conn.name,
        unit: processorUnit,
        id: processorUnit
      }

      return {
        ctls: getOrCreateController(ctls, addr.controllerId, config, lookup),
        edgeList: [...edgeList, buildEdge(processorUnit, ctlAddress, procAddress, conn, isInbound)]
      }
    },
    { ctls: initialCtls, edgeList: [] } as ProcessedBinding
  )

export const toConfigurationGraph = (config: Configuration, lookup: Lookup): ConfigurationGraph => {
  const { controllers, processingUnits, edges } = config.processors.reduce(
    (acc, processor, idx) => {
      const unit = lookup.units[processor.unit]
      if (!unit) return acc

      const bindings = collectBindings(processor, unit)
      const { ctls, edgeList } = processBindings(
        config,
        processor.unit,
        bindings,
        acc.controllers,
        lookup
      )

      return {
        controllers: ctls,
        processingUnits: [...acc.processingUnits, buildProcessorNode(config, processor, unit, idx)],
        edges: [...acc.edges, ...edgeList]
      }
    },
    { controllers: {}, processingUnits: [], edges: [] } as GraphAccumulator
  )

  return {
    id: config.id,
    name: config.name,
    description: config.description,
    controllers,
    processingUnits,
    edges
  }
}
