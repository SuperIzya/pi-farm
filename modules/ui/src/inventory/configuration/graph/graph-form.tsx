import React from 'react'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'
import { Background, Controls, ReactFlow, ReactFlowProvider } from '@xyflow/react'
import * as styles from './graph-form.scss'
import { UnitsList } from './units-list'
import { getNewEntity } from '../selectors'
import { GraphEdge, GraphNode } from '../types'
import { ControllerNode, ProcessingUnitNode } from './nodes'
import { DnDProvider } from './useDnD'

const nodeTypes = { processingUnit: ProcessingUnitNode, controller: ControllerNode }

type InnerGraphFormProps = {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

const InnerGraphForm = ({ nodes, edges }: InnerGraphFormProps) => (
  <ReactFlow
    id='graph-canvas'
    nodes={nodes}
    edges={edges}
    onNodesChange={() => console.log('nodes changed')}
    onEdgesChange={() => console.log('edges changed')}
    onConnect={() => console.log('connect')}
    onNodeClick={() => console.log('node clicked')}
    nodeTypes={nodeTypes}
    fitView
  >
    <Background />
    <Controls />
  </ReactFlow>
)


const mapStateToProps = createSelector(getNewEntity, newEntity => ({
  nodes: [
    ...Object.values(newEntity?.controllers ?? {}),
    ...Object.values(newEntity?.processingUnits ?? {})
  ],
  edges: newEntity?.edges || []
}))
const GraphForm = connect(mapStateToProps)(InnerGraphForm)

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


