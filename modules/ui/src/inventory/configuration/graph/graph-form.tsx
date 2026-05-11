import React from 'react'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'
import { Background, Controls, FinalConnectionState, ReactFlow, ReactFlowProvider } from '@xyflow/react'
import * as styles from './graph-form.scss'
import { UnitsList } from './units-list'
import { getNewEntity } from '../selectors'
import { GraphEdge, GraphNode } from '../types'
import { ControllerNode, ProcessingUnitNode } from './nodes'
import { DnDProvider } from './useDnD'
import { addEdge } from '../actions'

const nodeTypes = { processingUnit: ProcessingUnitNode, controller: ControllerNode }

type GraphInners = {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

type InnerGraphFormProps = GraphInners & {
  addEdge: (connection: FinalConnectionState) => void
}

const InnerGraphForm = ({ nodes, edges, addEdge }: InnerGraphFormProps) => (
  <ReactFlow
    id='graph-canvas'
    nodes={nodes}
    edges={edges}
    onConnectEnd={(_, state) => addEdge(state)}
    nodeTypes={nodeTypes}
    nodeOrigin={[0.5, 0.5]}
    fitView
  >
    <Background />
    <Controls />
  </ReactFlow>
)


const mapStateToProps = connect(createSelector(getNewEntity, newEntity => ({
  nodes: [
    ...Object.values(newEntity?.controllers ?? {}),
    ...Object.values(newEntity?.processingUnits ?? {})
  ],
  edges: newEntity?.edges || []
})))

const mapAddEdge = connect(null, (dispatch, { nodes, edges }: GraphInners) => ({
  addEdge: (connection: FinalConnectionState) => {
    const { fromHandle, fromNode, toHandle, toNode } = connection
    if(!fromHandle || !toHandle || !fromNode || !toNode) return
    const sourceNode = nodes.find(node => node.data.id === fromNode.data.id)
    const targetNode = nodes.find(node => node.data.id === toNode.data.id)
    if (!sourceNode || !targetNode) return
    const sourceEndpoint = sourceNode.data.endpoints.find(endpoint => fromHandle.id?.startsWith(`(${endpoint.name})_(${endpoint.units})_(${endpoint.type})_${endpoint.direction}`))
    const targetEndpoint = targetNode.data.endpoints.find(endpoint => toHandle.id?.startsWith(`(${endpoint.name})_(${endpoint.units})_(${endpoint.type})_${endpoint.direction}`))
    if (!sourceEndpoint || !targetEndpoint) return
    const isInvalid = (sourceEndpoint.direction === targetEndpoint.direction && targetEndpoint.direction !== 'both') ||
      sourceEndpoint.units !== targetEndpoint.units ||
      sourceEndpoint.type !== targetEndpoint.type ||
      sourceNode.type === targetNode.type ||
      edges.some(edge =>
        (edge.source === fromNode.data.id && edge.sourceHandle === fromHandle.id) ||
        (edge.target === toNode.data.id && edge.targetHandle === toHandle.id)
      )

    if (isInvalid) return

    const srcHdl = sourceEndpoint.direction === 'out' ? fromHandle : toHandle
    const tgtHdl = targetEndpoint.direction === 'in' ? toHandle : fromHandle
    const srcNode = sourceEndpoint.direction === 'out' ? sourceNode : targetNode
    const tgtNode = targetEndpoint.direction === 'in' ? targetNode : sourceNode

    dispatch(addEdge({
      id: `edge-(${srcNode.id}-${srcHdl.id})-(${tgtNode.id}-${tgtHdl.id})`,
      source: srcNode.id,
      target: tgtNode.id,
      sourceHandle: srcHdl.id,
      targetHandle: tgtHdl.id,
      type: 'default',
      animated: true
    }))
  }
}))

const GraphForm = mapStateToProps(mapAddEdge(InnerGraphForm))

export const Graph = () => (
  <ReactFlowProvider>
    <DnDProvider>
      <div className={styles.container}>
        <div className={styles.sidebar}>
          <UnitsList />
        </div>
        <div className={styles.canvas}>
          <GraphForm />
        </div>    
      </div>
    </DnDProvider>
  </ReactFlowProvider>
)


