import React from 'react'
import {
  GenericList,
  GenericListProps,
  ItemProps,
  ListOuterProps,
  WithItemKey
} from '../../utils/list-mixin'
import * as styles from './periphery-list.scss'
import { getKnownEntities, sortPeripheriesKeys } from './selectors'
import { RootState } from './types'
import classNames from 'classnames'
import { ControllerType, IdType, Peripheries, PeripheryType } from '../../types'
import {
  getKnownEntities as getKnownPeriphery,
  getIsLoading as getPeripheryLoading,
  usePTSelector
} from '../periphery-types/selectors'
import { WaitLoading } from '../../utils/wait-loading'
import { Text } from '../../utils/text'
import { buildItemSelector } from '../store-mixin'

type PeripheryIndex = { idx: IdType }

type PeripheryListProps = ListOuterProps & PeripheryIndex

type InnerItemProps = PeripheryIndex & {}

type PeripheryItemProps = ItemProps<InnerItemProps>

const getPeripheries = <T,>(idx: number, f: (peripheries: Peripheries) => T) =>
  buildItemSelector(getKnownEntities)(idx, (ct: ControllerType) => f(ct.peripheries))

const getPeripheriesAndKeys = (idx: number) =>
  getPeripheries(idx, peripheries => ({
    keys: sortPeripheriesKeys(Object.typedKeys(peripheries)),
    peripheries
  }))

const getPeriphery =
  <T,>(itemKey: number, idx: number, f: (p: PeripheryType | undefined) => T) =>
  (s: RootState) => {
    const { keys, peripheries } = getPeripheriesAndKeys(idx)(s)
    const p = peripheries[keys[itemKey]]
    const entities = getKnownPeriphery(s)
    return f(entities.find(({ id }) => id === p))
  }

const mapPeripheryKey = (itemKey: number, idx: number) =>
  getPeriphery(itemKey, idx, p => ({ keyName: p?.name ?? '' }))

const mapImage = (itemKey: number, idx: number) =>
  getPeriphery(itemKey, idx, p => ({ image: p?.image ?? '', name: p?.name ?? '' }))

const mapName = (itemKey: number, idx: number) =>
  getPeriphery(itemKey, idx, p => ({ name: p?.name ?? '' }))

const mapCount = (idx: number) => (s: RootState) => getPeripheriesAndKeys(idx)(s).keys.length

const PeripheryKey = ({ idx, itemKey }: { idx: number } & WithItemKey) => {
  const { keyName } = usePTSelector(mapPeripheryKey(itemKey, idx))
  return <Text text={keyName} className={styles.peripheryKey} />
}

const PeripheryImage = ({ idx, itemKey }: { idx: number } & WithItemKey) => {
  const { image, name } = usePTSelector(mapImage(itemKey, idx))
  return image ? <img className={styles.peripheryImage} src={image} alt={name} /> : null
}

const PeripheryName = ({ idx, itemKey }: { idx: number } & WithItemKey) => {
  const { name } = usePTSelector(mapName(itemKey, idx))
  return <Text className={styles.peripheryName} text={name} />
}

const PeripheryItem = ({ itemKey, idx }: PeripheryItemProps) => (
  <div className={styles.peripheryItem}>
    <PeripheryKey itemKey={itemKey} idx={idx} />
    <PeripheryName itemKey={itemKey} idx={idx} />
    <PeripheryImage itemKey={itemKey} idx={idx} />
  </div>
)
const List = (props: Omit<GenericListProps<PeripheryIndex>, 'count'>) => {
  const count = usePTSelector(mapCount(props.idx))
  return <GenericList {...props} count={count} />
}

export const PeripheryList = ({ containerClassName, listConfigCss, idx }: PeripheryListProps) => (
  <WaitLoading isLoadingSelector={getPeripheryLoading}>
    <List
      idx={idx}
      Item={PeripheryItem}
      containerClassName={classNames(styles.container, containerClassName)}
      listConfigCss={{
        ...listConfigCss,
        columns: 2,
        columnMax: '1fr',
        columnMin: 'auto'
      }}
    />
  </WaitLoading>
)
