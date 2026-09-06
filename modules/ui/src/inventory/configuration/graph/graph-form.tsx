import React, { Dispatch } from 'react'
import type { Controller } from '../../../types'
import { useDispatch, useSelector } from 'react-redux'
import { createSelector } from 'reselect'
import {
  Background,
  Controls,
  EdgeChange,
  FinalConnectionState,
  NodeTypes,
  ReactFlow,
  ReactFlowProvider
} from '@xyflow/react'
import * as styles from './graph-form.scss'
import { UnitsList } from './units-list'
import { getNewEntity } from '../selectors'
import type { Endpoint, GraphEdge, GraphNode, RootState } from '../types'
import { DnDProvider } from './useDnD'
import { addEdge, removeEdge, selectEdge } from '../actions'
import type { DataConnection, ProcessingUnit, Selector } from '../../../types'
import { ProcessingNode, ControllerNode } from './nodes'
import { UnknownAction } from '@reduxjs/toolkit'
import { getAllProcessingUnits } from '../selectors'
import { getKnownEntities as getAllControllers } from '../../controller/selectors'

const nodeTypes = (
  puSelector: (id: string) => Selector<ProcessingUnit, RootState>,
  ctlSelector: (id: string) => Selector<Controller, RootState>
) => ({
  processingUnit: ProcessingNode(puSelector),
  controller: ControllerNode(ctlSelector)
})

type GraphInners = {
  nodes: GraphNode[]
  edges: GraphEdge[]
  nodeTypes: NodeTypes
}

type InnerGraphFormProps = GraphInners & {
  addEdge: (connection: FinalConnectionState) => void
  onEdgesChange: (changes: EdgeChange[]) => void
}

const InnerGraphForm = ({
  nodeTypes,
  nodes,
  edges,
  addEdge,
  onEdgesChange
}: InnerGraphFormProps) => (
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

const addDispatch = (
  dispatch: Dispatch<UnknownAction>,
  { nodes, edges }: Omit<GraphInners, 'nodeTypes'>
) => ({
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
      || !(
        ('controller' in srcEp && 'processor' in tgtEp)
        || ('processor' in srcEp && 'controller' in tgtEp)
      )

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
      if ('processor' in source && 'controller' in target) {
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
})

const GraphForm = () => {
  const selector = createSelector(getNewEntity, newEntity => ({
    nodes: [...Object.values(newEntity?.controllers ?? {}), ...(newEntity?.processingUnits ?? [])],
    edges: newEntity?.edges || []
  }))

  const data = useSelector.withTypes<RootState>()(selector)
  const actions = addDispatch(useDispatch(), data)
  const puSelector = (id: string) => createSelector(getAllProcessingUnits, units => units[id])
  const ctlSelector = (id: string) =>
    createSelector(getAllControllers, controllers => {
      const ctrlId = parseInt(id, 10)
      return controllers.find(c => c.id === ctrlId)!
    })

  return <InnerGraphForm {...data} {...actions} nodeTypes={nodeTypes(puSelector, ctlSelector)} />
}

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
