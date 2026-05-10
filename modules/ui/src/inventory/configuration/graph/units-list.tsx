import React from 'react'
import * as styles from './units-list.scss'
import ArrowForwardIosSharpIcon from '@mui/icons-material/ArrowForwardIosSharp'
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined'
import { GenericList, GenericListProps, ListItem, WithItemKey, getListKey } from '../../../utils/list-mixin'
import { getAllProcessingUnits, getProcessingUnitsIsLoading } from '../selectors'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'
import { Text } from '../../../utils/text'
import { getKnownEntities as getControllers } from '../../controller/selectors'
import { WaitLoading } from '../../../utils/wait-loading'
import Accordion from '@mui/material/Accordion'
import MuiAccordionSummary from '@mui/material/AccordionSummary'
import AccordionDetails from '@mui/material/AccordionDetails'
import classNames from 'classnames'
import { composeRoutes, RouteNames } from '../../../utils/routes'
import { DragData, OnDropAction, useDnD, useDnDPosition } from './useDnD'
import { Dispatch } from 'redux'
import { addControllerNode, addProcessorNode } from '../actions'
import type { ControllerNode } from './nodes'
import type { ProcessingNode } from '../types'
import type { XYPosition } from '@xyflow/react'
import { ControllerId } from '../../../types'

type DnDNode<T> = {
  onDragStart: (id: T, itemKey: number) => (event: React.PointerEvent<HTMLDivElement>) => void
}

const processingUnitsListSelector = createSelector(getAllProcessingUnits, units =>
  Object.values(units)
)

const mapPUName = () =>
  createSelector(processingUnitsListSelector, getListKey, (units, key) => ({
    name: units[key].name
  }))

const PUName = connect(mapPUName)(({ name, onDragStart, itemKey }: { name: string } & DnDNode<string> & WithItemKey) => (
  <div className={styles.item} onPointerDown={onDragStart(name, itemKey)}>
    <Text className={styles.name} text={name} />
  </div>
))

const PUItem: ListItem<DnDNode<string>> = ({ itemKey, onDragStart }) => (
  <PUName itemKey={itemKey} onDragStart={onDragStart} />
)


const mapPUCount = createSelector(processingUnitsListSelector, units => ({
  count: units.length
}))

const PUList = connect(mapPUCount)((props: GenericListProps<DnDNode<string>>) => <GenericList {...props} />)

const mapCtlName = () =>
  createSelector(getControllers, getListKey, (controllers, index) => ({
    name: controllers[index].name
  }))

const CtlName = connect(mapCtlName)(({ name }: { name: string }) => (
  <Text className={styles.name} text={name} />
))

const mapCtlId = () =>
  createSelector(getControllers, getListKey, (controllers, index) => ({
    id: controllers[index].typeId
  }))

const CtlLink = ({ id }: { id: ControllerId }) => (
  <a
    href={`${composeRoutes(RouteNames.base, RouteNames.inventory, RouteNames.controller)}/edit/${id}`}
    target='_blank'
    rel='noopener noreferrer'
  >
    <OpenInNewOutlinedIcon />
  </a>
)

type CtlItemProps = { id: ControllerId } & DnDNode<ControllerId> & WithItemKey
const CtlItemInner = connect(mapCtlId)(({ id, itemKey, onDragStart }: CtlItemProps) => (
  <div className={styles.item} onPointerDown={onDragStart(id, itemKey)}>
    <CtlName itemKey={itemKey} />
    <CtlLink id={id} />
  </div>
))

const CtlItem: ListItem<DnDNode<ControllerId>> = ({ itemKey, onDragStart }) =>  (
  <CtlItemInner itemKey={itemKey} onDragStart={onDragStart} />
)

const mapCtlCount = createSelector(getControllers, controllers => ({
  count: controllers.length
}))

const CtlList = connect(mapCtlCount)((props: GenericListProps<DnDNode<ControllerId>>) => <GenericList {...props} />)

type Section = 'processingUnits' | 'controllers'

type AccProps = {
  section: Section
  onChange: () => void
  startDraggingCtl: (id: ControllerId, itemKey: number) => (event: React.PointerEvent<HTMLDivElement>) => void
  startDraggingPU: (name: string, itemKey: number) => (event: React.PointerEvent<HTMLDivElement>) => void
}

const transition = { transition: { timeout: 300 } }

const UnitsListAcc = ({ section, onChange, startDraggingCtl, startDraggingPU }: AccProps) => (
  <div className={styles.container}>
    <WaitLoading isLoadingSelector={getProcessingUnitsIsLoading}>
      <Accordion
        className={classNames(
          styles.accordion,
          section === 'processingUnits' && styles.accordionExpanded
        )}
        expanded={section === 'processingUnits'}
        onChange={onChange}
        slotProps={transition}
      >
        <MuiAccordionSummary
          className={classNames(
            styles.accordionSummary,
            section === 'processingUnits' && styles.accordionSummaryExpanded
          )}
          expandIcon={<ArrowForwardIosSharpIcon sx={{ fontSize: '0.9rem' }} />}
        >
          <h2>Processing Units</h2>
        </MuiAccordionSummary>
        <AccordionDetails>
          <PUList
            containerClassName={styles.list}
            onDragStart={startDraggingPU}
            Item={PUItem}
            listConfigCss={{
              columns: 1,
              overflow: 'hidden'
            }}
          />
        </AccordionDetails>
      </Accordion>
      <Accordion
        className={classNames(
          styles.accordion,
          section === 'controllers' && styles.accordionExpanded
        )}
        expanded={section === 'controllers'}
        onChange={onChange}
        slotProps={transition}
      >
        <MuiAccordionSummary
          className={classNames(
            styles.accordionSummary,
            section === 'controllers' && styles.accordionSummaryExpanded
          )}
          expandIcon={<ArrowForwardIosSharpIcon sx={{ fontSize: '0.9rem' }} />}
        >
          <h2>Controllers</h2>
        </MuiAccordionSummary>
        <AccordionDetails>
          <CtlList
            containerClassName={styles.list}
            onDragStart={startDraggingCtl}
            Item={CtlItem}
            listConfigCss={{
              columns: 1,
              overflow: 'hidden'
            }}
          />
        </AccordionDetails>
      </Accordion>
    </WaitLoading>
  </div>
)

type GhostProps = {
  dragData: DragData
  position: XYPosition
}

const positionToCss = (position: XYPosition) => ({ '--x': `${position.x}px`, '--y': `${position.y}px` })

const GhostPU = ({dragData, position}: GhostProps) => (
  <div className={styles.dragged} style={positionToCss(position)}>
    <PUItem onDragStart={() => () => {}} itemKey={dragData.itemKey} />
  </div>
)

const GhostCtl = ({dragData, position}: GhostProps) => (
  <div className={styles.dragged} style={positionToCss(position)}>
    <CtlItem onDragStart={() => () => {}} itemKey={dragData.itemKey} />
  </div>
)

const GhostItem = () => {
  const { position } = useDnDPosition()
  const { dragData } = useDnD()
  if (!dragData || !position) return null
  return dragData.type === 'processingUnit' ?
    <GhostPU dragData={dragData} position={position} /> :
    <GhostCtl dragData={dragData} position={position} />
}

const mapDispatch = (dispatch: Dispatch) => ({
  addControllerNode: (node: ControllerNode) => dispatch(addControllerNode(node)),
  addProcessorNode: (node: ProcessingNode) => dispatch(addProcessorNode(node))
})

type DispatchProps = {
  addControllerNode: (node: ControllerNode) => void
  addProcessorNode: (node: ProcessingNode) => void
}

export const UnitsList = connect(null, mapDispatch)(({ addControllerNode, addProcessorNode }: DispatchProps) => {
  const [section, setSection] = React.useState<Section>('processingUnits')

  const onChange = () => {
    setSection(prev => (prev === 'processingUnits' ? 'controllers' : 'processingUnits'))
  }

  const { onDragStart } = useDnD()


  const createPUNode: (id: string) => OnDropAction = id => ({ position }) => addProcessorNode({
    id,
    type: 'processingUnit',
    data: { processingUnitId: id },
    position
  })

  const createCtlNode: (id: ControllerId) => OnDropAction = id => ({ position }) => addControllerNode({
    id: id.toString(),
    type: 'controller',
    data: { controllerId: id },
    position
  })

  const startDraggingPU = (id: string, itemKey: number) => (event: React.PointerEvent<HTMLDivElement>) =>
    onDragStart(event, { type: 'processingUnit', itemKey }, createPUNode(id))
   
  const startDraggingCtl = (id: ControllerId, itemKey: number) => (event: React.PointerEvent<HTMLDivElement>) =>
    onDragStart(event, { type: 'controller', itemKey }, createCtlNode(id))

  return (
    <>
      <UnitsListAcc section={section} onChange={onChange} startDraggingCtl={startDraggingCtl} startDraggingPU={startDraggingPU} />
      <GhostItem />
    </>
  )
})
