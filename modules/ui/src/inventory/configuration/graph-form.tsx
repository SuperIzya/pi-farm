import React from 'react'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'
import {
  Node,
  Edge,
  addEdge,
  Connection,
  Background,
  Controls,
  Handle,
  Position,
  NodeChange,
  EdgeChange,
  ReactFlow
} from '@xyflow/react'
import { Box } from '@mui/material'
import * as styles from './graph-form.scss'
import { ProcessingUnitsList } from './processing-units-list'
import { SlotModal, SlotNodeData } from './slot-modal'
import { setNewEntityNodes } from './actions'
import { getNewEntity, getProcessingUnits } from './selectors'
import type { ProcessingUnit } from '../../types'

const Description = ({ description }: { description?: string }) =>
  description && <div className={styles.description}>{description}</div>


const ProcessingUnitNode = ({ data }: { data: ProcessingUnit }) => (
  <div className={styles.processingUnitNode}>
    { data.inbound.map((v, idx) => (
      <Handle key={`inbound-${idx}`} type="target" position={Position.Top} id={v.name} />
    ))}
    <div className={styles.text}>
      <strong>{data.name}</strong>
      <Description description={data.description} />
    </div>
    { data.outbound.map((v, idx) => (
      <Handle key={`outbound-${idx}`} type="source" position={Position.Bottom} id={v.name} />
    ))}
  </div>
)

const SlotLabel = ({
  controllerName,
  peripheryTypeName
}: {
  controllerName?: string
  peripheryTypeName?: string
}) => (controllerName ? `${controllerName}\n${peripheryTypeName}` : '+ Select')

const SlotNode = ({ data }: { data: SlotNodeData }) => (
  <Box className={`${styles.slotNode} ${data.controllerId ? styles.slotNodeFilled : ''}`}>
    <Handle
      type={data.direction === 'in' ? 'target' : 'source'}
      position={data.direction === 'in' ? Position.Top : Position.Bottom}
    />
    <div className={styles.slotLabel}>
      <SlotLabel
        controllerName={data.controllerName}
        peripheryTypeName={data.peripheryTypeName}
      />
    </div>
  </Box>
)

const nodeTypes = { processingUnit: ProcessingUnitNode, slot: SlotNode }

type Pos = { x: number; y: number }

/**
 * Compute positions for inbound and outbound slot nodes.
 * Returns a mapping of slot key to {x, y} position.
 */
const computeSlotPositions = (
  puPos: Pos,
  inboundCount: number,
  outboundCount: number
): Record<string, Pos> => {
  const slotSpacing = 150
  const slotYOffset = 150

  const inboundPositions = Array.from({ length: inboundCount }).reduce<
    Record<string, Pos>
  >((acc, _, i) => {
    const startX = puPos.x - ((inboundCount - 1) * slotSpacing) / 2
    return {
      ...acc,
      [`slot-in-${i}`]: { x: startX + i * slotSpacing, y: puPos.y - slotYOffset }
    }
  }, {})

  const outboundPositions = Array.from({ length: outboundCount }).reduce<
    Record<string, { x: number; y: number }>
  >((acc, _, i) => {
    const startX = puPos.x - ((outboundCount - 1) * slotSpacing) / 2
    return {
      ...acc,
      [`slot-out-${i}`]: { x: startX + i * slotSpacing, y: puPos.y + slotYOffset }
    }
  }, {})

  return { ...inboundPositions, ...outboundPositions }
}

type InnerGraphFormProps = {
  nodes: Node[]
  edges: Edge[]
  processingUnits: Record<string, ProcessingUnit>
  onNodesEdgesChange: (nodes: Node[], edges: Edge[]) => void
  onSlotSelected: (slotNode: Node<SlotNodeData>) => void
}

const combineNodes = (
  pu: ProcessingUnit,
  nodes: Node[],
  puNode: Node,
  slotPositions: Record<string, Pos>
): Node[] => [
  ...nodes,
  puNode,
  ...pu.inbound.reduce<Node<SlotNodeData>[]>((acc, conn, idx) => {
    const slotId = `slot-in-${puNode.id}-${idx}`
    return [
      ...acc,
      {
        id: slotId,
        type: 'slot',
        position: slotPositions[`slot-in-${idx}`],
        data: {
          slotIndex: idx,
          direction: 'in',
          expectedType: conn.type,
          expectedUnits: conn.units
        }
      }
    ]
  }, []),
  ...pu.outbound.reduce<Node<SlotNodeData>[]>((acc, conn, idx) => {
    const slotId = `slot-out-${puNode.id}-${idx}`
    return [
      ...acc,
      {
        id: slotId,
        type: 'slot',
        position: slotPositions[`slot-out-${idx}`],
        data: {
          slotIndex: idx,
          direction: 'out',
          expectedType: conn.type,
          expectedUnits: conn.units
        }
      }
    ]
  }, [])
]

const combineEdges = (pu: ProcessingUnit, edges: Edge[], puNode: Node): Edge[] => [
  ...edges,
  ...pu.inbound.reduce<Edge[]>(
    (acc, _, idx) => [
      ...acc,
      {
        id: `edge-in-${puNode.id}-${idx}`,
        source: `slot-in-${puNode.id}-${idx}`,
        target: puNode.id
      }
    ],
    []
  ),
  ...pu.outbound.reduce<Edge[]>(
    (acc, _, idx) => [
      ...acc,
      {
        id: `edge-out-${puNode.id}-${idx}`,
        source: puNode.id,
        target: `slot-out-${puNode.id}-${idx}`
      }
    ],
    []
  )
]
const InnerGraphForm = ({
  nodes,
  edges,
  processingUnits,
  onNodesEdgesChange,
  onSlotSelected
}: InnerGraphFormProps) => {
  const [modalOpen, setModalOpen] = React.useState(false)
  const [selectedSlotNode, setSelectedSlotNode] = React.useState<
    Node<SlotNodeData> | undefined
  >()

  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    const puName = event.dataTransfer?.getData('text/plain')
    if (!puName) return

    const pu = processingUnits[puName]
    if (!pu) return

    const puPos = { x: 300, y: 300 }
    const puNode: Node = {
      id: `pu-${Date.now()}`,
      type: 'processingUnit',
      position: puPos,
      data: { name: pu.name, description: pu.description }
    }

    const slotPositions = computeSlotPositions(
      puPos,
      pu.inbound.length,
      pu.outbound.length
    )

    const newNodes = combineNodes(pu, nodes, puNode, slotPositions)

    const newEdges = combineEdges(pu, edges, puNode)

    onNodesEdgesChange(newNodes, newEdges)
  }

  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    event.dataTransfer.dropEffect = 'move'
  }

  const handleNodesChange = (changes: NodeChange[]) => {
    const updatedNodes = changes.reduce((acc, change) => {
      if (change.type === 'position' && 'position' in change) {
        return acc.map((n) =>
          n.id === change.id ? { ...n, position: change.position || n.position } : n
        )
      }
      return acc
    }, nodes)
    onNodesEdgesChange(updatedNodes, edges)
  }

  const handleEdgesChange = (changes: EdgeChange[]) => {
    const updatedEdges = changes.reduce((acc, change) => {
      if (change.type === 'remove') {
        return acc.filter((e) => e.id !== change.id)
      }
      return acc
    }, edges)
    onNodesEdgesChange(nodes, updatedEdges)
  }

  const handleConnect = (connection: Connection) => {
    onNodesEdgesChange(nodes, addEdge(connection, edges))
  }

  const handleNodeClick = (_: React.MouseEvent, node: Node) => {
    if (node.type === 'slot') {
      setSelectedSlotNode(node as Node<SlotNodeData>)
      setModalOpen(true)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.sidebar}>
        <ProcessingUnitsList />
      </div>
      <Box className={styles.canvas} onDrop={handleDrop} onDragOver={handleDragOver}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={handleNodesChange}
          onEdgesChange={handleEdgesChange}
          onConnect={handleConnect}
          onNodeClick={handleNodeClick}
          nodeTypes={nodeTypes}
          fitView
        >
          <Background />
          <Controls />
        </ReactFlow>
      </Box>
      <SlotModal
        open={modalOpen}
        slotNode={selectedSlotNode}
        onClose={() => {
          setModalOpen(false)
          setSelectedSlotNode(undefined)
        }}
      />
    </div>
  )
}

const mapStateToProps = createSelector(
  [getNewEntity, getProcessingUnits],
  (newEntity, processingUnits) => ({
    nodes: (newEntity?.nodes || []) as Node[],
    edges: (newEntity?.edges || []) as Edge[],
    processingUnits
  })
)

const mapDispatchToProps = (dispatch: any) => ({
  onNodesEdgesChange: (nodes: Node[], edges: Edge[]) => {
    dispatch(setNewEntityNodes({ nodes, edges }))
  },
  onSlotSelected: () => {}
})

export const GraphForm = connect(mapStateToProps, mapDispatchToProps)(InnerGraphForm)
