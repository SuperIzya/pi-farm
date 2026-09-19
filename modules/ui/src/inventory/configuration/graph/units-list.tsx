import React from 'react'
import * as styles from './units-list.scss'
import ArrowForwardIosSharpIcon from '@mui/icons-material/ArrowForwardIosSharp'
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined'
import {
  GenericList,
  GenericListProps,
  ListItem,
  WithKey,
} from '../../../utils/list-mixin'
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
  dispatchAddControllers,
  dispatchAddProcessors,
  WithDragData,
  withDragData,
  WithStartDrag,
  withStartDrag
} from './useDnD'
import type { XYPosition } from '@xyflow/react'
import type { Controller, ControllerId, ProcessingUnit, WithSelector } from '../../../types'
import { controllersEndpointsSelector, processorsEndpointsSelector } from './selectors'
import type { RootState } from '../types'

type NodeProps<T> = WithStartDrag & WithSelector<T, RootState>

const useULSelector = useSelector.withTypes<RootState>()

const processingUnitsListSelector = createSelector(getAllProcessingUnits, units =>
  Object.values(units)
)

const PUName = withStartDrag(({ onDragStart, selector }: NodeProps<ProcessingUnit>) => {
  const getData = createSelector(
    selector,
    processorsEndpointsSelector(selector),
    (p, { endpoints }) => ({ name: p?.name || '', endpoints })
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
          { type: 'processingUnit', selector },
          addNode({
            id: `${name}-${crypto.randomUUID()}`,
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

const PUItem: ListItem<ProcessingUnit, RootState> = ({ selector }) => (
  <PUName selector={selector} />
)

type ListProps = Omit<GenericListProps<ProcessingUnit, RootState>, 'count' | 'selectorFactory'>

const PUList = (props: ListProps) => {
  const count = useULSelector(state => processingUnitsListSelector(state).length)
  const itemSelector = (idx: number) => createSelector(processingUnitsListSelector, units => units[idx])

  return <GenericList<ProcessingUnit, RootState> {...props} count={count} selectorFactory={itemSelector} />
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
  ({ onDragStart, selector: nodeSelector }: NodeProps<Controller>) => {
    const dispatch = useDispatch()
    const addNode = dispatchAddControllers(dispatch)
    const selector = createSelector(
      nodeSelector,
      controllersEndpointsSelector(nodeSelector),
      (p, { endpoints }) => ({ id: p?.id || 0, name: p?.name || '', endpoints })
    )

    const { id, name, endpoints } = useULSelector(selector)

    return (
      <div
        className={styles.item}
        onPointerDown={(event: React.PointerEvent<HTMLDivElement>) =>
          onDragStart(event, { type: 'controller', selector: nodeSelector }, addNode({ id, endpoints }))
        }
      >
        <CtlName name={name} />
        <CtlLink id={id} />
      </div>
    )
  }
)

const CtlItem = ({ selector }: WithSelector<Controller, RootState>) => (
  <CtlItemInner selector={selector} />
)

type CtlListProps = Omit<GenericListProps<Controller, RootState>, 'count' | 'selectorFactory'>

const CtlList = (props: CtlListProps) => {
  const count = useULSelector(state => getControllers(state).length)
  const ctlSelector = (idx: number) => createSelector(getControllers, units => units[idx])
  return <GenericList<Controller, RootState> {...props} count={count} selectorFactory={ctlSelector} />
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

type GhostProps<T extends object> = {
  position: XYPosition
} & WithSelector<T, RootState>

const positionToCss = (position: XYPosition) => ({
  '--x': `${position.x}px`,
  '--y': `${position.y}px`
})

const GhostPU = ({ position, selector }: GhostProps<ProcessingUnit>) => (
  <div className={styles.dragged} style={positionToCss(position)}>
    <PUItem selector={selector} />
  </div>
)

const GhostCtl = ({ position, selector }: GhostProps<Controller>) => (
  <div className={styles.dragged} style={positionToCss(position)}>
    <CtlItem selector={selector} />
  </div>
)

const GhostItem = withDragData(({ position, dragData }: WithDragData) => {
  return dragData.type === 'processingUnit' ? (
    <GhostPU selector={dragData.selector} position={position} />
  ) : (
    <GhostCtl selector={dragData.selector} position={position} />
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
