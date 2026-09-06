import { Handle, NodeProps, Position } from '@xyflow/react'
import React, { useState } from 'react'
import * as styles from './nodes.scss'
import type {
  Controller,
  ControllerId,
  FlowDirection,
  ProcessingUnit,
  Selector,
  SelectorProps
} from '../../../types'
import DeleteForeverIcon from '@mui/icons-material/DeleteForever'
import OpenWithIcon from '@mui/icons-material/OpenWith'
import TuneIcon from '@mui/icons-material/Tune'
import { removeControllerNode, removeProcessorNode } from '../actions'
import Tooltip from '@mui/material/Tooltip'
import { GenericButton } from '../../form-mixin'
import {
  dispatchAddControllers,
  dispatchAddProcessors,
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
  RootState,
  ExtractNodeData,
  ProcessingUnitData
} from '../types'
import { ParamsDialog } from './params-dialog'
import { useDispatch, useSelector } from 'react-redux'

type WithActions<T, N extends NodeType> = WithAddNode<N> & {
  onDelete: (id: T) => void
}

const useTSelector = useSelector.withTypes<RootState>()

type LeafProps<T> = SelectorProps<T, RootState>

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

const Name = ({ name }: { name: string }) => <div className={styles.name}>{name}</div>

const Description = ({ description }: { description: string }) => (
  <div className={styles.description}>{description}</div>
)

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
            id={`(${v.name})_(${v.units})_(${v.type})_${direction}`}
            className={styles.handle}
            style={{ '--x': `${((idx + 1) / (length + 1)) * 100}%` }}
          />
        </Tooltip>
      ))}
  </div>
)

const PUName = ({ selector }: LeafProps<ProcessingUnit>) => {
  const name = useTSelector(state => selector(state)?.name || '')
  return <Name name={name} />
}

const PUDescription = ({ selector }: LeafProps<ProcessingUnit>) => {
  const description = useTSelector(state => selector(state)?.description || '')

  return <Description description={description} />
}

const ParamsButton = ({
  data,
  selector
}: { data: ProcessingUnitData } & LeafProps<ProcessingUnit>) => {
  const hasParams = useTSelector(
    state => Object.keys(selector(state)?.paramsSchema ?? []).length > 0
  )
  if (!hasParams) return null

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
        selector={selector}
        onClose={() => setParamsOpen(false)}
        processorId={data.id}
        unit={data.unit}
        currentParams={data.parameters}
      />
    </>
  )
}

type DragNodeProps<T, N extends NodeType> = {
  children: React.ReactElement[]
  nodeType: N
  data: ExtractNodeData<N>
  value: T
} & WithActions<T, N>
  & WithStartDrag

const DragNode = <T, N extends NodeType>() =>
  withStartDrag(
    ({ addNode, onDragStart, children, nodeType, data, value, onDelete }: DragNodeProps<T, N>) => (
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
          onClick={() => onDelete(value)}
          Icon={() => <DeleteForeverIcon />}
        />
        {children}
      </div>
    )
  )

export const ProcessingNode =
  (selectorFactory: (id: string) => Selector<ProcessingUnit, RootState>) =>
  ({ data, id }: NodeProps<ProcessingNodeType>) => {
    const dispatch = useDispatch()
    const DragProcessorNode = DragNode<string, 'processingUnit'>()
    const selector = selectorFactory(id)

    return (
      <DragProcessorNode
        nodeType='processingUnit'
        data={data}
        value={data.id}
        onDelete={(id: string) => dispatch(removeProcessorNode(id))}
        addNode={dispatchAddProcessors(dispatch)}
      >
        <HandleList endpoints={data.endpoints} direction='in' position={Position.Top} />
        <div className={styles.text}>
          <PUName selector={selector} />
          <PUDescription selector={selector} />
        </div>
        <HandleList endpoints={data.endpoints} direction='out' position={Position.Bottom} />
        <ParamsButton selector={selector} data={data} />
      </DragProcessorNode>
    )
  }
const ControllerName = ({ selector }: LeafProps<Controller>) => {
  const name = useTSelector(state => selector(state)?.name || '')
  return <Name name={name} />
}

const ControllerDescription = ({ selector }: LeafProps<Controller>) => {
  const description = useTSelector(state => selector(state)?.description || '')
  return <Description description={description} />
}

export const ControllerNode =
  (selectorFactory: (id: string) => Selector<Controller, RootState>) =>
  ({ data, id }: NodeProps<ControllerNodeType>) => {
    const dispatch = useDispatch()
    const DragControllerNode = DragNode<ControllerId, 'controller'>()

    const selector = selectorFactory(id)

    return (
      <DragControllerNode
        nodeType='controller'
        data={data}
        value={data.id}
        onDelete={(id: ControllerId) => dispatch(removeControllerNode(id))}
        addNode={dispatchAddControllers(dispatch)}
      >
        <HandleList endpoints={data.endpoints} direction='out' position={Position.Bottom} />
        <div className={styles.text}>
          <ControllerName selector={selector} />
          <ControllerDescription selector={selector} />
        </div>
        <HandleList endpoints={data.endpoints} direction='in' position={Position.Top} />
        <HandleList endpoints={data.endpoints} direction='both' position={Position.Left} />
      </DragControllerNode>
    )
  }
