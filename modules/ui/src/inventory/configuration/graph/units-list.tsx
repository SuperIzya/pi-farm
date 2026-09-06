import React from 'react'
import * as styles from './units-list.scss'
import ArrowForwardIosSharpIcon from '@mui/icons-material/ArrowForwardIosSharp'
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined'
import { GenericList, GenericListProps, ListItem, WithItemKey } from '../../../utils/list-mixin'
import { getAllProcessingUnits, getProcessingUnitsIsLoading } from '../selectors'
import { useDispatch, useSelector } from 'react-redux'
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
  dispatchAddControllers,
  dispatchAddProcessors,
  WithDragData,
  withDragData,
  WithStartDrag,
  withStartDrag
} from './useDnD'
import type { XYPosition } from '@xyflow/react'
import type { Controller, ControllerId, ProcessingUnit, SelectorProps } from '../../../types'
import { controllersEndpointsSelector, processorsEndpointsSelector } from './selectors'
import type { RootState } from '../types'

type NodeProps<T> = WithItemKey & WithStartDrag & SelectorProps<T, RootState>

const useULSelector = useSelector.withTypes<RootState>()

const processingUnitsListSelector = createSelector(getAllProcessingUnits, units =>
  Object.values(units)
)

const puSelector = (itemKey: number) =>
  createSelector(processingUnitsListSelector, units => units[itemKey])

const PUName = withStartDrag(({ itemKey, onDragStart, selector }: NodeProps<ProcessingUnit>) => {
  const getData = createSelector(
    selector,
    processorsEndpointsSelector(selector),
    ({ name }, { endpoints }) => ({ name, endpoints })
  )

  const { endpoints, name } = useULSelector(state => getData(state))
  const dispatch = useDispatch()
  const addNode = dispatchAddProcessors(dispatch)

  return (
    <div
      className={styles.item}
      onPointerDown={(event: React.PointerEvent<HTMLDivElement>) =>
        onDragStart(
          event,
          { type: 'processingUnit', itemKey },
          addNode({
            id: `${name}-${crypto.randomUUID()}`,
            itemKey,
            endpoints,
            unit: name,
            parameters: {}
          })
        )
      }
    >
      <Text className={styles.name} text={name} />
    </div>
  )
})

const PUItem: ListItem = ({ itemKey }) => (
  <PUName itemKey={itemKey} selector={puSelector(itemKey)} />
)

type ListProps = Omit<GenericListProps, 'count'>

const PUList = (props: ListProps) => {
  const count = useULSelector(state => processingUnitsListSelector(state).length)

  return <GenericList {...props} count={count} />
}

const CtlName = ({ name }: { name: string }) => <Text className={styles.name} text={name} />

const CtlLink = ({ id }: { id: ControllerId }) => (
  <a
    href={`${composeRoutes(RouteNames.base, RouteNames.inventory, RouteNames.controller)}/edit/${id}`}
    rel='noopener noreferrer'
  >
    <OpenInNewOutlinedIcon />
  </a>
)

const CtlItemInner = withStartDrag(
  ({ itemKey, onDragStart, selector: nodeSelector }: NodeProps<Controller>) => {
    const dispatch = useDispatch()
    const addNode = dispatchAddControllers(dispatch)
    const selector = createSelector(
      nodeSelector,
      controllersEndpointsSelector(nodeSelector),
      ({ id, name }, { endpoints }) => ({ id, name, endpoints })
    )

    const { id, name, endpoints } = useULSelector(selector)

    return (
      <div
        className={styles.item}
        onPointerDown={(event: React.PointerEvent<HTMLDivElement>) =>
          onDragStart(event, { type: 'controller', itemKey }, addNode({ id, itemKey, endpoints }))
        }
      >
        <CtlName name={name} />
        <CtlLink id={id} />
      </div>
    )
  }
)

const ctlSelector = (itemKey: number) => createSelector(getControllers, units => units[itemKey])
const CtlItem: ListItem = ({ itemKey }) => (
  <CtlItemInner itemKey={itemKey} selector={ctlSelector(itemKey)} />
)

type CtlListProps = Omit<GenericListProps, 'count'>

const CtlList = (props: CtlListProps) => {
  const count = useULSelector(state => getControllers(state).length)
  return <GenericList {...props} count={count} />
}

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
