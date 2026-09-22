import React from 'react'
import type {
  Controller,
  ControllerId,
  IdType,
  WithSelector as TWithSelector,
  Selector as TSelector
} from '../../types'
import { useSendCommand } from '../../client'
import * as styles from './list.scss'
import { DeleteButton, EditButton } from '../../utils/form-mixin'
import type { ClassName } from '../../types'
import { getIsLoading, getKnownEntities, useCtlSelector } from './selectors'
import { getKnownEntities as knownControllerTypes } from '../controller-types/selectors'
import { GenericList, type GenericListProps, ListItem } from '../../utils/list-mixin'
import { Text } from '../../utils/text'
import { PeripheryList } from '../controller-types/periphery-list'
import { setLoading } from './actions'
import type { RootState } from './types'
import { DescriptionIcon, TypeIcon } from '../../utils/icons'
import { ExportData, InventoryPage } from '../page'
import { createSelector } from '@reduxjs/toolkit'

type Selector<T = Controller> = TSelector<T, RootState>
type WithSelector<T = Controller> = TWithSelector<T, RootState>

const controllerTypeSelector = (selector: Selector) =>
  createSelector(
    selector,
    knownControllerTypes,
    (controller, knownTypes) => controller && knownTypes.find(({ id }) => id === controller.typeId)
  )

const Name = ({ selector, className }: WithSelector & ClassName) => {
  const name = useCtlSelector(createSelector(selector, p => p?.name || ''))
  return <Text className={className} text={name} />
}

const TypeName = ({ selector, className }: WithSelector & ClassName) => {
  const text = useCtlSelector(createSelector(controllerTypeSelector(selector), t => t?.name ?? ''))
  return <Text className={className} text={text} title='Type' />
}

const Description = ({ selector, className }: WithSelector & ClassName) => {
  const description = useCtlSelector(createSelector(selector, p => p?.description ?? ''))
  return <Text className={className} text={description} icon={<DescriptionIcon />} />
}

const TypeDescription = ({ selector, className }: WithSelector & ClassName) => {
  const text = useCtlSelector(
    createSelector(controllerTypeSelector(selector), t => t?.description ?? '')
  )
  return <Text className={className} text={text} icon={<TypeIcon />} />
}

type WithDelete = {
  sendDelete: (id: IdType) => void
}

const EditBtn = ({ selector }: TWithSelector<ControllerId, RootState>) => {
  const id = useCtlSelector(selector)
  return <EditButton className={styles.editButton} id={id ?? -1} />
}

const DeleteBtn = ({
  selector,
  sendDelete
}: TWithSelector<ControllerId, RootState> & WithDelete) => {
  const id = useCtlSelector(selector)
  return (
    <DeleteButton
      id={id ?? -1}
      className={styles.deleteButton}
      onDelete={sendDelete}
      isLoading={setLoading}
      itemName={'periphery type'}
    />
  )
}

const Item: ListItem<Controller, RootState, WithDelete> = ({ selector, sendDelete }) => {
  const idSelector = createSelector(selector, p => p?.id)
  const id = useCtlSelector(idSelector)
  const ctSelector = createSelector(selector, knownControllerTypes, (item, types) =>
    types.find(t => t.id === item?.typeId)
  )
  return (
    <div className={styles.item}>
      <Name selector={selector} className={styles.name} />
      <Description selector={selector} className={styles.description} />
      <TypeName selector={selector} className={styles.typeName} />
      <TypeDescription selector={selector} className={styles.typeDescription} />
      <PeripheryList
        containerClassName={styles.plist}
        selector={ctSelector}
        listConfigCss={{
          columns: 3,
          maxWidth: '100%',
          columnMin: '50px',
          columnMax: '75px'
        }}
      />
      <ExportData className={styles.exportButton} id={id ?? -1} command={'controller'} />
      <EditBtn selector={idSelector} />
      <DeleteBtn sendDelete={sendDelete} selector={idSelector} />
    </div>
  )
}

const List = (
  p: Omit<GenericListProps<Controller, RootState, WithDelete>, 'count' | 'selectorFactory'>
) => {
  const count = useCtlSelector(s => (getKnownEntities(s) || []).length)
  const selector = (idx: number) => createSelector(getKnownEntities, entities => entities[idx])
  return (
    <GenericList<Controller, RootState, WithDelete>
      {...p}
      count={count}
      selectorFactory={selector}
    />
  )
}

export const InnerList = () => {
  const send = useSendCommand()
  const sendDelete = (id: IdType) => send('delete-controller', id)
  return (
    <InventoryPage<RootState>
      styles={styles}
      getIsLoading={getIsLoading}
      getDataCommands={['get-controllers', 'get-controller-types', 'get-periphery-types']}
      title={'List of controllers'}
      addEntityText={'Add new controller'}
    >
      <List
        containerClassName={styles.list}
        sendDelete={sendDelete}
        Item={Item}
        listConfigCss={{
          itemMaxHeight: '400px',
          columnMin: 'min-content',
          columnMax: 'auto'
        }}
      />
    </InventoryPage>
  )
}
