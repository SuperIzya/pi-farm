import React from 'react'
import {
  GenericList,
  GenericListProps,
  getListKey,
  ItemProps,
  ListOuterProps
} from '../../utils/list-mixin'
import * as styles from './periphery-list.scss'
import { getKnownEntities, sortPeripheriesKeys } from './selectors'
import { RootState } from './types'
import { connect } from 'react-redux'
import classNames from 'classnames'
import { IdType, Peripheries, PeripheryType } from '../../types'
import { createSelector } from 'reselect'
import {
  getKnownEntities as getKnownPeriphery,
  getIsLoading as getPeripheryLoading
} from '../periphery-types/selectors'
import { WaitLoading } from '../../utils/wait-loading'
import { Text } from '../../utils/text'

type PeripheryIndex = { idx: IdType }

type PeripheryListProps = ListOuterProps & PeripheryIndex

type InnerItemProps = PeripheryIndex & {}

type PeripheryItemProps = ItemProps<InnerItemProps>

const getControllerIndex = (state: RootState, { idx }: PeripheryIndex) => idx
const getPeripheries = <T,>(f: (p: Peripheries) => T) =>
  createSelector([getKnownEntities, getControllerIndex], (entities, index) =>
    f(entities[index].peripheries)
  )

const getPeripheriesAndKeys = () =>
  getPeripheries(peripheries => ({
    keys: sortPeripheriesKeys(Object.typedKeys(peripheries)),
    peripheries
  }))

const getPeriphery = <T,>(f: (p: PeripheryType | undefined) => T) =>
  createSelector(
    [getKnownPeriphery, getPeripheriesAndKeys(), getListKey],
    (entities, { keys, peripheries }, itemKey) =>
      f(entities.find(({ id }) => id === peripheries[keys[itemKey]]))
  )
const mapPeripheryKey = connect(() =>
  createSelector([getPeripheriesAndKeys(), getListKey], ({ keys }, itemKey) => ({
    keyName: keys[itemKey]
  }))
)

const mapImage = connect(() => getPeriphery(p => ({ image: p?.image ?? '', name: p?.name ?? '' })))

const mapName = connect(() => getPeriphery(p => ({ name: p?.name ?? '' })))

const mapCount = connect(() =>
  createSelector([getPeripheriesAndKeys()], ({ keys }) => ({ count: keys.length }))
)
const PeripheryKey = mapPeripheryKey(({ keyName }: { keyName: string }) => (
  <div className={styles.peripheryKey}>
    <span>{keyName}</span>
  </div>
))
type ImageProps = {
  image?: string
  name: string
}
const PeripheryImage = mapImage(
  ({ image, name }: ImageProps) =>
    image && <img className={styles.peripheryImage} src={image} alt={name} />
)
const PeripheryName = mapName(({ name }: { name: string }) => (
  <Text className={styles.peripheryName} text={name} />
))

const PeripheryItem = ({ itemKey, idx }: PeripheryItemProps) => (
  <div className={styles.peripheryItem}>
    <PeripheryKey itemKey={itemKey} idx={idx} />
    <PeripheryName itemKey={itemKey} idx={idx} />
    <PeripheryImage itemKey={itemKey} idx={idx} />
  </div>
)
const List = mapCount((props: GenericListProps<PeripheryIndex>) => <GenericList {...props} />)

export const PeripheryList = ({ containerClassName, listConfigCss, idx }: PeripheryListProps) => (
  <WaitLoading isLoadingSelector={getPeripheryLoading}>
    <List
      idx={idx}
      Item={PeripheryItem}
      containerClassName={classNames(styles.container, containerClassName)}
      listConfigCss={listConfigCss}
    />
  </WaitLoading>
)
