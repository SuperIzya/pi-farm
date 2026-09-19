import React from 'react'
import * as styles from './list.scss'
import { GenericList, GenericListProps, ListItem } from '../../utils/list-mixin'
import { getKnownEntities, getIsLoading, useCTSelector } from './selectors'
import { EditButton, DeleteButton } from '../../utils/form-mixin'
import { setLoading } from './actions'
import { useSendCommand } from '../../client'
import { Text } from '../../utils/text'
import type { ControllerType, ControllerTypeId, IdType, WithSelector } from '../../types'
import { PeripheryList } from './periphery-list'
import { DescriptionIcon, CodeIcon } from '../../utils/icons'
import { ExportData, InventoryPage } from '../page'
import { RootState } from './types'
import { createSelector } from '@reduxjs/toolkit'

type WithDelete = {
  sendDelete: (id: number) => void
}

type CTItemSelector = WithSelector<ControllerType, RootState>

const Name = ({ selector }: CTItemSelector) => {
  const name = useCTSelector(createSelector(selector, p => p?.name ?? ''))
  return <Text className={styles.name} text={name} />
}

const Description = ({ selector }: CTItemSelector) => {
  const description = useCTSelector(createSelector(selector, p => p?.description ?? ''))
  return <Text className={styles.description} text={description} icon={<DescriptionIcon />} />
}

const Schema = ({ selector }: CTItemSelector) => {
  const schema = useCTSelector(createSelector(selector, p => p?.schema ?? ''))
  return (
    schema && (
      <a href={schema} target='_blank' rel='noreferrer'>
        Schema
      </a>
    )
  )
}

const Code = ({ selector }: CTItemSelector) => {
  const code = useCTSelector(createSelector(selector, p => p?.code ?? ''))
  return <Text className={styles.code} text={code} icon={<CodeIcon />} />
}

const EditBtn = ({ selector }: WithSelector<ControllerTypeId, RootState>) => {
  const id = useCTSelector(selector)
  return <EditButton id={id ?? -1} className={styles.editButton} />
}

const DeleteBtn = ({
  selector,
  sendDelete
}: WithSelector<ControllerTypeId, RootState> & WithDelete) => {
  const id = useCTSelector(selector)
  return (
    <DeleteButton
      id={id ?? -1}
      className={styles.deleteButton}
      onDelete={sendDelete}
      isLoading={setLoading}
      itemName={'controller type'}
    />
  )
}

const Item: ListItem<ControllerType, RootState, WithDelete> = ({ selector, sendDelete }) => {
  const idSelector = createSelector(selector, p => p?.id)
  const id = useCTSelector(idSelector)
  return (
    <div className={styles.item}>
      <Name selector={selector} />
      <Description selector={selector} />
      <Code selector={selector} />
      <Schema selector={selector} />
      <PeripheryList
        selector={selector}
        containerClassName={styles.plist}
        listConfigCss={{
          columns: 4,
          maxWidth: '100%',
          columnMin: '50px',
          columnMax: '75px'
        }}
      />
      <ExportData className={styles.exportButton} id={id ?? -1} command={'controller-type'} />
      <EditBtn selector={idSelector} />
      <DeleteBtn sendDelete={sendDelete} selector={idSelector} />
    </div>
  )
}

const List = (
  props: Omit<GenericListProps<ControllerType, RootState, WithDelete>, 'count' | 'selectorFactory'>
) => {
  const count = useCTSelector(s => (getKnownEntities(s) || []).length)
  const selector = (idx: number) => createSelector(getKnownEntities, entities => entities[idx])
  return (
    <GenericList<ControllerType, RootState, WithDelete>
      {...props}
      count={count}
      selectorFactory={selector}
    />
  )
}

export const InnerList = () => {
  const send = useSendCommand()
  const sendDelete = (id: IdType) => send('delete-controller-type', id)
  return (
    <InventoryPage<RootState>
      styles={styles}
      getDataCommand={'get-controller-types'}
      getIsLoading={getIsLoading}
      title={'List of controller types'}
      addEntityText={'Add new controller type'}
    >
      <List
        containerClassName={styles.list}
        sendDelete={sendDelete}
        Item={Item}
        listConfigCss={{
          itemMaxHeight: '400px',
          columns: 3,
          columnMin: 'min-content',
          columnMax: 'auto'
        }}
      />
    </InventoryPage>
  )
}
