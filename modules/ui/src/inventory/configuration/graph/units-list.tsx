import React from 'react'
import * as styles from './units-list.scss'
import ArrowForwardIosSharpIcon from '@mui/icons-material/ArrowForwardIosSharp'
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined'
import {
  GenericList,
  GenericListProps,
  ListItem,
  WithItemKey,
  getListKey
} from '../../../utils/list-mixin'
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
import {
  DragData,
  WithAddNode,
  withAddControllers,
  withAddProcessors,
  WithDragData,
  withDragData,
  WithStartDrag,
  withStartDrag
} from './useDnD'
import type { XYPosition } from '@xyflow/react'
import type { ControllerId } from '../../../types'
import { getControllersEndpoints, getProcessorsEndpoints } from './selectors'
import type { ExtractNodeData, NodeType } from '../types'

type NodeProps<TN extends NodeType> = WithItemKey
  & WithStartDrag
  & WithAddNode<TN>
  & ExtractNodeData<TN>

const processingUnitsListSelector = createSelector(getAllProcessingUnits, units =>
  Object.values(units)
)

const mapPUName = connect(() =>
  createSelector(processingUnitsListSelector, getListKey, (units, key) => ({ unit: units[key].name, id: units[key].name, parameters: {} }))
)

const PUName = mapPUName(
  getProcessorsEndpoints(
    withAddProcessors(
      withStartDrag(
        ({ unit, onDragStart, itemKey, endpoints, addNode }: NodeProps<'processingUnit'>) => (
          <div
            className={styles.item}
            onPointerDown={(event: React.PointerEvent<HTMLDivElement>) =>
              onDragStart(
                event,
                { type: 'processingUnit', itemKey },
                addNode({
                  id: `${unit}-${crypto.randomUUID()}`,
                  itemKey,
                  endpoints,
                  unit,
                  parameters: {}
                })
              )
            }
          >
            <Text className={styles.name} text={unit} />
          </div>
        )
      )
    )
  )
)

const PUItem: ListItem = ({ itemKey }) => <PUName itemKey={itemKey} />

const mapPUCount = createSelector(processingUnitsListSelector, units => ({
  count: units.length
}))

const PUList = connect(mapPUCount)((props: GenericListProps) => <GenericList {...props} />)

const mapCtlName = () =>
  createSelector(getControllers, getListKey, (controllers, index) => ({
    name: controllers[index].name
  }))

const CtlName = connect(mapCtlName)(({ name }: { name: string }) => (
  <Text className={styles.name} text={name} />
))

const mapCtlId = connect(() =>
  createSelector(getControllers, getListKey, (controllers, index) => ({
    id: controllers[index].id
  }))
)

const CtlLink = ({ id }: { id: ControllerId }) => (
  <a
    href={`${composeRoutes(RouteNames.base, RouteNames.inventory, RouteNames.controller)}/edit/${id}`}
    rel='noopener noreferrer'
  >
    <OpenInNewOutlinedIcon />
  </a>
)

const CtlItemInner = mapCtlId(
  getControllersEndpoints(
    withAddControllers(
      withStartDrag(
        ({
          id,
          itemKey,
          onDragStart,
          endpoints,
          addNode
        }: NodeProps<'controller'>) => (
          <div
            className={styles.item}
            onPointerDown={(event: React.PointerEvent<HTMLDivElement>) =>
              onDragStart(
                event,
                { type: 'controller', itemKey },
                addNode({ id, itemKey, endpoints })
              )
            }
          >
            <CtlName itemKey={itemKey} />
            <CtlLink id={id} />
          </div>
        )
      )
    )
  )
)

const CtlItem: ListItem = ({ itemKey }) => <CtlItemInner itemKey={itemKey} />

const mapCtlCount = connect(
  createSelector(getControllers, controllers => ({
    count: controllers.length
  }))
)

const CtlList = mapCtlCount((props: GenericListProps) => <GenericList {...props} />)

type Section = 'processingUnits' | 'controllers'

type AccProps = {
  section: Section
  onChange: () => void
}

const transition = { transition: { timeout: 300 } }

const UnitsListAcc = ({ section, onChange }: AccProps) => (
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

const positionToCss = (position: XYPosition) => ({
  '--x': `${position.x}px`,
  '--y': `${position.y}px`
})

const GhostPU = ({ dragData, position }: GhostProps) => (
  <div className={styles.dragged} style={positionToCss(position)}>
    <PUItem itemKey={dragData.itemKey} />
  </div>
)

const GhostCtl = ({ dragData, position }: GhostProps) => (
  <div className={styles.dragged} style={positionToCss(position)}>
    <CtlItem itemKey={dragData.itemKey} />
  </div>
)

const GhostItem = withDragData(({ position, dragData }: WithDragData) => {
  return dragData.type === 'processingUnit' ? (
    <GhostPU dragData={dragData} position={position} />
  ) : (
    <GhostCtl dragData={dragData} position={position} />
  )
})

export const UnitsList = () => {
  const [section, setSection] = React.useState<Section>('processingUnits')

  const onChange = () => {
    setSection(prev => (prev === 'processingUnits' ? 'controllers' : 'processingUnits'))
  }

  return (
    <>
      <UnitsListAcc section={section} onChange={onChange} />
      <GhostItem />
    </>
  )
}
