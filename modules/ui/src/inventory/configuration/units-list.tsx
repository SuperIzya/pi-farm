import React from 'react'
import * as styles from './units-list.scss'
import ArrowForwardIosSharpIcon from '@mui/icons-material/ArrowForwardIosSharp'
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined'
import { GenericList, GenericListProps, ListItem, WithItemKey, getListKey } from '../../utils/list-mixin'
import { getProcessingUnits, getProcessingUnitsIsLoading } from './selectors'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'
import { Text } from '../../utils/text'
import { getKnownEntities as getControllers } from '../controller/selectors'
import { WaitLoading } from '../../utils/wait-loading'
import Accordion from '@mui/material/Accordion'
import MuiAccordionSummary from '@mui/material/AccordionSummary'
import AccordionDetails from '@mui/material/AccordionDetails'
import classNames from 'classnames'
import { composeRoutes, RouteNames } from '../../utils/routes'

const processingUnitsListSelector = createSelector(getProcessingUnits, units =>
  Object.values(units)
)

const mapPUName = () => createSelector(
    processingUnitsListSelector,
    getListKey,
    (units, key) => ({ name: units[key].name })
  )

const PUName = connect(mapPUName)(({ name }: { name: string }) => (
  <Text className={styles.name} text={name} />
))

const PUItem: ListItem = ({ itemKey }) => (
  <div className={styles.item}>
    <PUName itemKey={itemKey} />
  </div>
)

const mapPUCount = createSelector(processingUnitsListSelector, units => ({
  count: units.length
}))

const PUList = connect(mapPUCount)((props: GenericListProps) => <GenericList {...props} />)

const mapCtlName = () => createSelector(
    getControllers,
    getListKey,
    (controllers, index) => ({ name: controllers[index].name })
  )

const CtlName = connect(mapCtlName)(({ name }: { name: string }) => (
  <Text className={styles.name} text={name} />
))

const mapCtlId = () => createSelector(
  getControllers,
  getListKey,
  (controllers, index) => ({ id: controllers[index].typeId })
)

const CtlLink = connect(mapCtlId)(({id}: {id: number}) => (
  <a href={`${composeRoutes(RouteNames.base, RouteNames.inventory, RouteNames.controller)}/edit/${id}`} target="_blank" rel="noopener noreferrer">
    <OpenInNewOutlinedIcon/>
  </a>
))

const CtlItem: ListItem = ({itemKey}) => (
  <div className={styles.item}>
    <CtlName itemKey={itemKey} />
    <CtlLink itemKey={itemKey} />
  </div>
)

const mapCtlCount = createSelector(getControllers, controllers => ({
  count: controllers.length
}))

const CtlList = connect(mapCtlCount)((props: GenericListProps) => <GenericList {...props} />)

type Section = 'processingUnits' | 'controllers'

type AccProps = {
  section: Section
  onChange: () => void
}

const transition = { transition: { timeout: 300 } }

const UnitsListAcc = ({ section, onChange}: AccProps) => (
   <div className={styles.container}>
    <WaitLoading isLoadingSelector={getProcessingUnitsIsLoading}>
      <Accordion
        className={classNames(styles.accordion, section === 'processingUnits' && styles.accordionExpanded)}
        expanded={section === 'processingUnits'}
        onChange={onChange}
        slotProps={transition}
        >
        <MuiAccordionSummary
          className={classNames(styles.accordionSummary, section === 'processingUnits' && styles.accordionSummaryExpanded)}
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
        className={classNames(styles.accordion, section === 'controllers' && styles.accordionExpanded)}
        expanded={section === 'controllers'}
        onChange={onChange}
        slotProps={transition}
      >
        <MuiAccordionSummary
          className={classNames(styles.accordionSummary, section === 'controllers' && styles.accordionSummaryExpanded)}
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

export const UnitsList = () => {
  const [section, setSection] = React.useState<Section>('processingUnits') 

  const onChange = () => {
    setSection(prev => prev === 'processingUnits' ? 'controllers' : 'processingUnits')
  }

  return <UnitsListAcc section={section} onChange={onChange} />
}


