import React from 'react'
import * as styles from './processing-units-list.scss'
import {
  GenericList,
  GenericListProps,
  ListItem,
  WithItemKey
} from '../../utils/list-mixin'
import { getProcessingUnits, getProcessingUnitsIsLoading } from './selectors'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'
import { Text } from '../../utils/text'
import { WaitLoading } from '../../utils/wait-loading'

const processingUnitsListSelector = createSelector([getProcessingUnits], (units) =>
  Object.values(units)
)

const processingUnitAtIndexSelector = createSelector(
  [processingUnitsListSelector, (_, { itemKey }: WithItemKey) => itemKey],
  (units, index) => units[index]
)

const mapName = () =>
  createSelector([processingUnitAtIndexSelector], (unit) => ({
    name: unit.name
  }))

const Name = connect(mapName)(({ name }: { name: string }) => (
  <Text className={styles.name} text={name} />
))

const Item: ListItem = ({ itemKey }) => (
  <div className={styles.item}>
    <Name itemKey={itemKey} />
  </div>
)

const mapCount = createSelector([processingUnitsListSelector], (units) => ({
  count: units.length
}))

const List = connect(mapCount)((props: GenericListProps) => <GenericList {...props} />)

export const ProcessingUnitsList = () => (
  <div className={styles.container}>
    <h2>Available Processing Units</h2>
    <WaitLoading isLoadingSelector={getProcessingUnitsIsLoading}>
      <List containerClassName={styles.list} Item={Item} />
    </WaitLoading>
  </div>
)
