import React from 'react'
import {
  CountSelector,
  createList,
  ItemProps,
  ListOuterProps
} from '../../../utils/list-mixin'
import * as styles from './periphery-list.scss'
import {
  getKnownEntities,
  getPeripheryImage,
  getPeripheryKey,
  getPeripheryName,
  sortPeripheriesKeys
} from './selectors'
import { RootState } from './types'
import { connect } from 'react-redux'
import classNames from 'classnames'

type PeripheryIndex = { idx: number }
type PeripheryItemProps = ItemProps<PeripheryIndex>

const PeripheryKey = connect(() => {
  const selector = getPeripheryKey()
  return (state: RootState, props: PeripheryItemProps) => ({
    keyName: selector(state, props)
  })
})(({ keyName }: { keyName: string }) => (
  <div className={styles.peripheryKey}>
    <span>{keyName}</span>
  </div>
))

const PeripheryImage = connect(() => {
  const imgSelector = getPeripheryImage()
  const nameSelector = getPeripheryName()
  return (state: RootState, props: PeripheryItemProps) => ({
    image: imgSelector(state, props),
    name: nameSelector(state, props)
  })
})(
  ({ image, name }: { image: string | null; name: string }) =>
    image && <img className={styles.peripheryImage} src={image} alt={name} />
)

const PeripheryName = connect(() => {
  const selector = getPeripheryName()
  return (state: RootState, props: PeripheryItemProps) => ({
    name: selector(state, props)
  })
})(({ name }: { name: string }) => <span className={styles.peripheryName}>{name}</span>)

const PeripheryItem = ({ itemKey, idx }: PeripheryItemProps) => (
  <div className={styles.peripheryItem}>
    <PeripheryKey itemKey={itemKey} idx={idx} />
    <PeripheryName itemKey={itemKey} idx={idx} />
    <PeripheryImage itemKey={itemKey} idx={idx} />
  </div>
)

const peripheryCountSelector: CountSelector<RootState, string, { idx: number }> = (
  state: RootState,
  { idx }: { idx: number }
) => sortPeripheriesKeys(Object.keys(getKnownEntities(state)[idx].peripheries))

const listCreator = createList(peripheryCountSelector)

const List = listCreator<PeripheryIndex>(PeripheryItem)

export const PeripheryList = ({
  containerClassName,
  listConfigCss,
  idx
}: ListOuterProps & PeripheryIndex) => (
  <List
    idx={idx}
    containerClassName={classNames(styles.container, containerClassName)}
    listConfigCss={listConfigCss}
  />
)
