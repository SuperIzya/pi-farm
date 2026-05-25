import { Handle, NodeProps, Position } from '@xyflow/react'
import React, { useState } from 'react'
import * as styles from './nodes.scss'
import type { ControllerId, FlowDirection } from '../../../types'
import DeleteForeverIcon from '@mui/icons-material/DeleteForever'
import OpenWithIcon from '@mui/icons-material/OpenWith'
import TuneIcon from '@mui/icons-material/Tune'
import {
  getProcessorName,
  getProcessorDescription,
  getControllerDescription,
  getControllerName,
  getProcessorHasParams
} from './selectors'
import { removeControllerNode, removeProcessorNode } from '../actions'
import { connect } from 'react-redux'
import Tooltip from '@mui/material/Tooltip'
import { GenericButton } from '../../form-mixin'
import {
  mapAddControllers,
  mapAddProcessors,
  WithAddNode,
  WithStartDrag,
  withStartDrag
} from './useDnD'
import IconButton from '@mui/material/IconButton'
import type {
  Endpoint,
  ControllerNode as ControllerNodeType,
  ProcessingNode as ProcessingNodeType,
  NodeType,
  ExtractNodeData,
  ProcessingUnitData
} from '../types'
import { ParamsDialog } from './params-dialog'

type WithActions<T, N extends NodeType> = WithAddNode<N> & {
  onDelete: (id: T) => void
}

const addDispatchController = connect(null, dispatch => ({
  onDelete: (id: ControllerId) => dispatch(removeControllerNode(id)),
  ...mapAddControllers(dispatch)
}))
const addDispatchProcessor = connect(null, dispatch => ({
  onDelete: (id: string) => dispatch(removeProcessorNode(id)),
  ...mapAddProcessors(dispatch)
}))

const Description = ({ description }: { description?: string }) =>
  description && <div className={styles.description}>{description}</div>

const Name = ({ name }: { name: string }) => <div className={styles.name}>{name}</div>

type HandleListProps = {
  endpoints: Endpoint[]
  direction: FlowDirection
  position: Position
}

const types: { [key in FlowDirection]: 'target' | 'source' } = {
  in: 'target',
  out: 'source',
  both: 'source'
}

const HandleList = ({ endpoints, direction, position }: HandleListProps) => (
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
            position={position}
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

type ParamsButtonProps = {
  hasParams: boolean
  data: ProcessingUnitData
}

const ParamsButtonReal = ({ data }: { data: ProcessingUnitData }) => {
  const [paramsOpen, setParamsOpen] = useState(false)
  return (
    <>
      <GenericButton
        className={styles.params}
        onClick={() => setParamsOpen(true)}
        Icon={() => <TuneIcon />}
      />
      <ParamsDialog
        open={paramsOpen}
        onClose={() => setParamsOpen(false)}
        processorId={data.id}
        unit={data.unit}
        currentParams={data.parameters}
      />
    </>
  )
}

const ParamsButtonSelect = ({ hasParams, data }: ParamsButtonProps) => hasParams ? <ParamsButtonReal data={data} /> : null

const ParamsButton = connect(getProcessorHasParams)(ParamsButtonSelect)

type DragNodeProps<T, N extends NodeType> = {
  children: React.ReactElement[]
  nodeType: N
  data: ExtractNodeData<N>
  extract: (data: ExtractNodeData<N>) => T
} & WithActions<T, N>
  & WithStartDrag

const DragNode = <T, N extends NodeType>() =>
  withStartDrag(
    ({
      addNode,
      onDragStart,
      children,
      nodeType,
      data,
      extract,
      onDelete
    }: DragNodeProps<T, N>) => (
      <div className={styles.node}>
        <div
          className={styles.dragHandle}
          onPointerDown={evt =>
            onDragStart(evt, { type: nodeType, itemKey: data.itemKey }, addNode(data))
          }
        >
          <IconButton>
            <OpenWithIcon />
          </IconButton>
        </div>
        <GenericButton
          className={styles.delete}
          onClick={() => onDelete(extract(data))}
          Icon={() => <DeleteForeverIcon />}
        />
        {children}
      </div>
    )
  )

const DragProcessorNode = addDispatchProcessor(DragNode<string, 'processingUnit'>())

export const ProcessingNode = ({ data }: NodeProps<ProcessingNodeType>) => (
    <DragProcessorNode nodeType='processingUnit' data={data} extract={data => data.id}>
      <HandleList endpoints={data.endpoints} direction='in' position={Position.Top} />
      <div className={styles.text}>
        <PUName unit={data.unit} />
        <PUDescription unit={data.unit} />
      </div>
      <HandleList endpoints={data.endpoints} direction='out' position={Position.Bottom} />
      <ParamsButton unit={data.unit} data={data} />      
    </DragProcessorNode>
  )


const ControllerName = connect(getControllerName)(Name)

const ControllerDescription = connect(getControllerDescription)(Description)

const DragControllerNode = addDispatchController(DragNode<ControllerId, 'controller'>())

export const ControllerNode = ({ data }: NodeProps<ControllerNodeType>) => (
  <DragControllerNode nodeType='controller' data={data} extract={data => data.id}>
    <HandleList endpoints={data.endpoints} direction='out' position={Position.Bottom} />
    <div className={styles.text}>
      <ControllerName id={data.id} />
      <ControllerDescription id={data.id} />
    </div>
    <HandleList endpoints={data.endpoints} direction='in' position={Position.Top} />
    <HandleList endpoints={data.endpoints} direction='both' position={Position.Left} />
  </DragControllerNode>
)
