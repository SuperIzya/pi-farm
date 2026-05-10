import { Handle, NodeProps, Position, Node } from '@xyflow/react'
import React from 'react'
import * as styles from './nodes.scss'
import type { ControllerId, PeripheryDirection } from '../../../types'
import {
  Endpoint,
  getProcessorsEndpoints,
  getControllersEndpoints,
  getProcessorName,
  getProcessorDescription,
  getControllerDescription,
  getControllerName
} from './selectors'
import { connect } from 'react-redux'
import Tooltip from '@mui/material/Tooltip'

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
    {endpoints.map((v, idx, {length}) => (
      <Tooltip 
        key={`${direction}-${idx}`}
        title={
          <div className={styles.tooltip}>
            <div className={styles.tooltipName}>{v.name}</div>
            <div className={styles.tooltipUnits}>{v.units} units</div>
            <div className={styles.tooltipType}>{v.type}</div>
          </div>
        }
        placement="top"
        arrow
        style={{ opacity: 0 }}
      >
        <Handle
          type={types[direction]}
          position={positions[direction]}
          id={v.name}
          className={styles.handle}
          style={{ '--x': `${(idx + 1) / (length + 1) * 100}%` }}
        />
      </Tooltip>
    ))}
  </div>
)

const PUHandles = connect(getProcessorsEndpoints)(HandleList)

const PUName = connect(getProcessorName)(Name)

const PUDescription = connect(getProcessorDescription)(Description)

export type ProcessingUnitNode = Node<{ processingUnitId: string }, 'processingUnit'>

export const ProcessingUnitNode = ({
  data: { processingUnitId }
}: NodeProps<ProcessingUnitNode>) => (
  <div className={styles.node}>
    <PUHandles processingUnitId={processingUnitId} direction='in' />
    <div className={styles.text}>
      <PUName processingUnitId={processingUnitId} />
      <PUDescription processingUnitId={processingUnitId} />
    </div>
    <PUHandles processingUnitId={processingUnitId} direction='out' />
  </div>
)

const ControllerHandles = connect(getControllersEndpoints)(HandleList)

const ControllerName = connect(getControllerName)(Name)

const ControllerDescription = connect(getControllerDescription)(Description)

export type ControllerNode = Node<{ controllerId: ControllerId }, 'controller'>

export const ControllerNode = ({ data: { controllerId } }: NodeProps<ControllerNode>) => (
  <div className={styles.node}>
    <ControllerHandles controllerId={controllerId} direction='in' />
    <div className={styles.text}>
      <ControllerName controllerId={controllerId} />
      <ControllerDescription controllerId={controllerId} />
    </div>
    <ControllerHandles controllerId={controllerId} direction='out' />
    <ControllerHandles controllerId={controllerId} direction='both' />
  </div>
)
