import React from 'react'
import { getImage } from '../periphery-types/selectors'
import {
  GenericList,
  GenericListProps,
  ItemProps,
  ListOuterProps,
  WithKey
} from '../../utils/list-mixin'
import * as styles from './periphery-list.scss'
import { sortPeripheriesKeys } from './selectors'
import { RootState } from './types'
import classNames from 'classnames'
import { ControllerType, Peripheries, PeripheryType, Selector, WithSelector } from '../../types'
import {
  getKnownEntities as getKnownPeriphery,
  getIsLoading as getPeripheryLoading,
  usePTSelector
} from '../periphery-types/selectors'
import { WaitLoading } from '../../utils/wait-loading'
import { Text } from '../../utils/text'
import { createSelector } from '@reduxjs/toolkit'

type PeripheryListProps = ListOuterProps

type WithKeysSelector<S extends RootState = RootState> = { keysSelector: KeysSelector<S> }

type PIProps<P extends object = PeripheryType, S extends RootState = RootState> = {
  selector: Selector<P, S>
} & WithKeysSelector<S>
type PeripheriesSelector<S extends RootState = RootState> = Selector<Peripheries, S>
type KeysSelector<S extends RootState = RootState> = (s: S) => string[]
type ItemType = { 
  key: string
  name: string
  image: string | undefined
}
type PeripherySelector<S extends RootState = RootState> = Selector<ItemType, S>
type LeafProps<S extends RootState = RootState> = WithSelector<ItemType, S>

const getPeriphery = <S extends RootState = RootState>(
  keysSelector: KeysSelector<S>,
  selector: PeripheriesSelector<S>
) => {
  return (key: number): PeripherySelector<S> =>
    createSelector(getKnownPeriphery, keysSelector, selector, (entities, keys, peripheries) => {
      const keyName = keys[key]
      const p = peripheries?.[keyName]
      const periphery = entities.find(({ id }) => id === p)
      return { key: keyName, name: periphery?.name ?? '', image: getImage(periphery) }
    })
}

const PeripheryKey = <S extends RootState = RootState>({ selector }: LeafProps<S>) => {
  const key = usePTSelector(createSelector(selector, p => p?.key ?? ''))
  return <Text text={key} className={styles.peripheryKey} />
}

const PeripheryImage = <S extends RootState = RootState>({ selector }: LeafProps<S>) => {
  const { image, name } = usePTSelector(
    createSelector(selector, p => ({
      name: p?.name ?? '',
      image: p?.image
    }))
  )
  return image ? (
    <div className={styles.peripheryImage}>
      <img src={image} alt={name} />
    </div>
  ) : null
}

const PeripheryName = <S extends RootState = RootState>({ selector }: LeafProps<S>) => {
  const name = usePTSelector(createSelector(selector, p => p?.name ?? ''))
  return <Text className={styles.peripheryName} text={name} />
}

const PeripheryItem = <S extends RootState = RootState>({
  selector
}: ItemProps<ItemType, S, PIProps<ItemType, S> & WithKey>) => {
  return (
    <div className={styles.peripheryItem}>
      <PeripheryKey selector={selector} />
      <PeripheryName selector={selector} />
      <PeripheryImage selector={selector} />
    </div>
  )
}

type ListProps<S extends RootState = RootState> = Omit<
  GenericListProps<ItemType, S, PIProps<ItemType, S>>,
  'count'
>

const List = <S extends RootState = RootState>(props: ListProps<S>) => {
  const count = usePTSelector(props.keysSelector).length
  return <GenericList<ItemType, S, PIProps<ItemType, S>> {...props} count={count} />
}

export const PeripheryList = <S extends RootState = RootState>({
  containerClassName,
  listConfigCss,
  selector
}: PeripheryListProps & WithSelector<ControllerType, S>) => {
  const peripheries = createSelector(selector, p => p?.peripheries ?? {})
  const keysSelector = createSelector(peripheries, p => sortPeripheriesKeys(Object.typedKeys(p)))
  const ptSelector = getPeriphery(keysSelector, peripheries)
  return (
    <WaitLoading isLoadingSelector={getPeripheryLoading}>
      <List
        selectorFactory={ptSelector}
        Item={PeripheryItem<S>}
        keysSelector={keysSelector}
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
}
