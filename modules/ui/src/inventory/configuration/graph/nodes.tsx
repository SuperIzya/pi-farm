import { Handle, NodeProps, Position, Node } from '@xyflow/react'
import React from 'react'
import * as styles from './nodes.scss'
import type { ControllerId, PeripheryDirection } from '../../../types'
import DeleteForeverIcon from '@mui/icons-material/DeleteForever'
import OpenWithIcon from '@mui/icons-material/OpenWith'
import {
  Endpoint,
  getProcessorName,
  getProcessorDescription,
  getControllerDescription,
  getControllerName
} from './selectors'
import { removeControllerNode, removeProcessorNode } from '../actions'
import { connect } from 'react-redux'
import Tooltip from '@mui/material/Tooltip'
import { GenericButton } from '../../form-mixin'
import { mapAddNodes, WithAddNode, WithStartDrag, withStartDrag } from './useDnD'
import IconButton from '@mui/material/IconButton'
import type { NodeData } from '../types'

type WithActions<T> = WithAddNode & {
  onDelete: (id: T) => void
}

const addDispatchController = connect(null, dispatch => ({
  onDelete: (id: ControllerId) => dispatch(removeControllerNode(id)),
  ...mapAddNodes(dispatch)
}))
const addDispatchProcessor = connect(null, dispatch => ({
  onDelete: (id: string) => dispatch(removeProcessorNode(id)),
  ...mapAddNodes(dispatch)
}))

const Description = ({ description }: { description?: string }) =>
  description && <div className={styles.description}>{description}</div>

const Name = ({ name }: { name: string }) => <div className={styles.name}>{name}</div>

type HandleListProps = {
  endpoints: Endpoint[]
  direction: PeripheryDirection
}

const positions: { [key in PeripheryDirection]: Position } = {
  in: Position.Top,
  out: Position.Bottom,
  both: Position.Left
}

const types: { [key in PeripheryDirection]: 'target' | 'source' } = {
  in: 'target',
  out: 'source',
  both: 'source'
}

const HandleList = ({ endpoints, direction }: HandleListProps) => (
  <div className={styles.handles}>
    {endpoints
      .filter(endpoint => endpoint.direction === direction)
      .map((v, idx, { length }) => (
        <Tooltip
          key={`${direction}-${idx}`}
          title={
            <div className={styles.tooltip}>
              <div className={styles.tooltipName}>{v.name}</div>
              <div className={styles.tooltipUnits}>{v.units} units</div>
              <div className={styles.tooltipType}>{v.type}</div>
            </div>
          }
          placement='top'
          arrow
          style={{ opacity: 0 }}
        >
          <Handle
            type={types[direction]}
            position={positions[direction]}
            id={`(${v.name})_(${v.units})_(${v.type})_${direction}#${idx}`}
            className={styles.handle}
            style={{ '--x': `${((idx + 1) / (length + 1)) * 100}%` }}
          />
        </Tooltip>
      ))}
  </div>
)

const PUName = connect(getProcessorName)(Name)

const PUDescription = connect(getProcessorDescription)(Description)

export type ProcessingUnitNode = Node<NodeData<string>, 'processingUnit'>

export const ProcessingUnitNode = addDispatchProcessor(
  withStartDrag(
    ({
      data: { id, itemKey, endpoints },
      onDelete,
      addProcessorNode,
      onDragStart
    }: NodeProps<ProcessingUnitNode> & WithActions<string> & WithStartDrag) => (
      <div className={styles.node}>
        <div
          className={styles.dragHandle}
          onPointerDown={evt =>
            onDragStart(
              evt,
              { type: 'processingUnit', itemKey },
              addProcessorNode(id, itemKey, endpoints)
            )
          }
        >
          <IconButton>
            <OpenWithIcon />
          </IconButton>
        </div>
        <GenericButton
          className={styles.delete}
          onClick={() => onDelete(id)}
          Icon={() => <DeleteForeverIcon />}
        />
        <HandleList endpoints={endpoints} direction='in' />
        <div className={styles.text}>
          <PUName id={id} />
          <PUDescription id={id} />
        </div>
        <HandleList endpoints={endpoints} direction='out' />
      </div>
    )
  )
)

const ControllerName = connect(getControllerName)(Name)

const ControllerDescription = connect(getControllerDescription)(Description)

export type ControllerNode = Node<NodeData<ControllerId>, 'controller'>

export const ControllerNode = addDispatchController(
  withStartDrag(
    ({
      data: { id, itemKey, endpoints },
      onDelete,
      addControllerNode,
      onDragStart
    }: NodeProps<ControllerNode> & WithActions<ControllerId> & WithStartDrag) => (
      <div className={styles.node}>
        <div
          className={styles.dragHandle}
          onPointerDown={evt =>
            onDragStart(
              evt,
              { type: 'controller', itemKey },
              addControllerNode(id, itemKey, endpoints)
            )
          }
        >
          <IconButton>
            <OpenWithIcon />
          </IconButton>
        </div>
        <GenericButton
          className={styles.delete}
          onClick={() => onDelete(id)}
          Icon={() => <DeleteForeverIcon />}
        />
        <HandleList endpoints={endpoints} direction='in' />
        <div className={styles.text}>
          <ControllerName id={id} />
          <ControllerDescription id={id} />
        </div>
        <HandleList endpoints={endpoints} direction='out' />
        <HandleList endpoints={endpoints} direction='both' />
      </div>
    )
  )
)
