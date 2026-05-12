import React from 'react'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'
import {
  Background,
  Controls,
  EdgeChange,
  FinalConnectionState,
  ReactFlow,
  ReactFlowProvider
} from '@xyflow/react'
import * as styles from './graph-form.scss'
import { UnitsList } from './units-list'
import { getNewEntity } from '../selectors'
import { GraphEdge, GraphNode } from '../types'
import { ControllerNode, ProcessingUnitNode } from './nodes'
import { DnDProvider } from './useDnD'
import { addEdge, removeEdge, selectEdge } from '../actions'
import { Endpoint } from './selectors'
import { DataConnection } from '../../../types'

const nodeTypes = { processingUnit: ProcessingUnitNode, controller: ControllerNode }

type GraphInners = {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

type InnerGraphFormProps = GraphInners & {
  addEdge: (connection: FinalConnectionState) => void
  onEdgesChange: (changes: EdgeChange[]) => void
}

const InnerGraphForm = ({ nodes, edges, addEdge, onEdgesChange }: InnerGraphFormProps) => (
  <ReactFlow
    id='graph-canvas'
    nodes={nodes}
    edges={edges}
    onConnectEnd={(_, state) => addEdge(state)}
    onEdgesChange={onEdgesChange}
    nodeTypes={nodeTypes}
    nodeOrigin={[0.5, 0.5]}
    deleteKeyCode={['Delete', 'Backspace']}
    fitView
  >
    <Background />
    <Controls />
  </ReactFlow>
)

const mapStateToProps = connect(
  createSelector(getNewEntity, newEntity => ({
    nodes: [
      ...Object.values(newEntity?.controllers ?? {}),
      ...Object.values(newEntity?.processingUnits ?? {})
    ],
    edges: newEntity?.edges || []
  }))
)

const mapAddEdge = connect(null, (dispatch, { nodes, edges }: GraphInners) => ({
  addEdge: (connection: FinalConnectionState) => {
    const { fromHandle, fromNode, toHandle, toNode } = connection
    if (!fromHandle || !toHandle || !fromNode || !toNode) return
    const sourceNode = nodes.find(node => node.data.id === fromNode.data.id)
    const targetNode = nodes.find(node => node.data.id === toNode.data.id)
    if (!sourceNode || !targetNode) return
    const sourceEndpoint = sourceNode.data.endpoints.find(endpoint =>
      fromHandle.id?.startsWith(
        `(${endpoint.name})_(${endpoint.units})_(${endpoint.type})_${endpoint.direction}`
      )
    )
    const targetEndpoint = targetNode.data.endpoints.find(endpoint =>
      toHandle.id?.startsWith(
        `(${endpoint.name})_(${endpoint.units})_(${endpoint.type})_${endpoint.direction}`
      )
    )
    if (!sourceEndpoint || !targetEndpoint) return

    const srcHdl = sourceEndpoint.direction === 'out' ? fromHandle : toHandle
    const tgtHdl = targetEndpoint.direction === 'in' ? toHandle : fromHandle
    const srcNode = sourceEndpoint.direction === 'out' ? sourceNode : targetNode
    const tgtNode = targetEndpoint.direction === 'in' ? targetNode : sourceNode
    const srcEp = sourceEndpoint.direction === 'out' ? sourceEndpoint : targetEndpoint
    const tgtEp = targetEndpoint.direction === 'in' ? targetEndpoint : sourceEndpoint

    const isInvalid =
      (sourceEndpoint.direction === targetEndpoint.direction && targetEndpoint.direction !== 'both')
      || sourceEndpoint.units !== targetEndpoint.units
      || sourceEndpoint.type !== targetEndpoint.type
      || sourceNode.type === targetNode.type
      || edges.some(
        edge =>
          (edge.source === fromNode.data.id && edge.sourceHandle === fromHandle.id)
          || (edge.target === toNode.data.id && edge.targetHandle === toHandle.id)
      )
      || !(('controller' in srcEp && 'processor' in tgtEp) || ('processor' in srcEp && 'controller' in tgtEp))

    if (isInvalid) return

    const toConnectionData = (source: Endpoint, target: Endpoint): DataConnection => {
      if ('controller' in source && 'processor' in target) {
        return {
          from: source.controller,
          to: target.processor,
          units: source.units,
          type: source.type
        }
      }
      if('processor' in source && 'controller' in target) {
        return {
          from: source.processor,
          to: target.controller,
          units: source.units,
          type: source.type
        }
      }
      throw new Error('Unreachable code: invalid connection endpoints')
    }
    dispatch(
      addEdge({
        id: `edge-(${srcNode.id}-${srcHdl.id})-(${tgtNode.id}-${tgtHdl.id})`,
        source: srcNode.id,
        target: tgtNode.id,
        sourceHandle: srcHdl.id,
        targetHandle: tgtHdl.id,
        data: toConnectionData(srcEp, tgtEp),
        type: 'default',
        animated: true
      })
    )
  },
  onEdgesChange: (changes: EdgeChange[]) => {
    changes.forEach(change => {
      if (change.type === 'remove') {
        dispatch(removeEdge(change.id))
      } else if (change.type === 'select') {
        dispatch(selectEdge(change.id))
      }
    })
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
