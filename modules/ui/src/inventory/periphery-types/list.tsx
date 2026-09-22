import React from 'react'
import { getImage, getIsLoading, getKnownEntities, usePTSelector } from './selectors'
import { GenericList, GenericListProps, ItemProps, type ListItem } from '../../utils/list-mixin'
import * as rawStyles from './list.scss'
import { DeleteButton, EditButton } from '../../utils/form-mixin'
import type { FlowDirection, PeripheryType } from '../../types'
import { useSendCommand } from '../../client'
import { setLoading } from './actions'
import { Text } from '../../utils/text'
import { ConnectionsList } from './connections'
import { buildItemSelector } from '../store-mixin'
import { DescriptionIcon } from '../../utils/icons'
import { ExportData, InventoryPage } from '../page'
import { RootState } from './types'
import { createSelector } from '@reduxjs/toolkit'

type Styles = typeof rawStyles
type PListStyles = { [key in keyof FlowDirection]: string } & Styles
const styles = rawStyles as PListStyles

type PTListItem = ListItem<PeripheryType, RootState, PeripheryItemProps>
type PTItemProps = ItemProps<PeripheryType, RootState>

const peripherySelector = buildItemSelector(getKnownEntities)

const PeripheryName = ({ selector }: PTItemProps) => {
  const name = usePTSelector(selector)?.name ?? ''
  return <Text className={styles.name} text={name} />
}

type ImageProps = {
  image: string
  name: string
}

const PImage = ({ image, name }: ImageProps) => (
  <div className={styles.image}>
    <img src={image} alt={`Periphery ${name}`} />
  </div>
)

const PeripheryImage = ({ selector }: PTItemProps) => {
  const { image, name } = usePTSelector(
    createSelector(selector, p => ({
      image: getImage(p),
      name: p?.name ?? ''
    }))
  )
  return <PImage image={image} name={name} />
}

const PeripheryDescription = ({ selector }: PTItemProps) => {
  const description = usePTSelector(createSelector(selector, p => p?.description ?? ''))
  return <Text className={styles.description} text={description} icon={<DescriptionIcon />} />
}

type WithIdSelector = {
  idSelector: (state: RootState) => number | undefined
}

const EditBtn = ({ idSelector }: WithIdSelector) => {
  const id = usePTSelector(idSelector)
  return <EditButton id={id ?? -1} className={styles.editButton} />
}

type PeripheryItemProps = {
  sendDelete: (id: number) => void
}

const DeleteBtn = ({ idSelector, sendDelete }: WithIdSelector & PeripheryItemProps) => {
  const id = usePTSelector(idSelector)
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

const PeripheryItem: PTListItem = ({ selector, sendDelete }) => {
  const idSelector = createSelector(selector, p => p?.id)
  const id = usePTSelector(idSelector)
  return (
    <div className={styles.item}>
      <PeripheryName selector={selector} />
      <PeripheryImage selector={selector} />
      <div className={styles.connections}>
        <ConnectionsList selector={selector} />
      </div>
      <PeripheryDescription selector={selector} />
      <ExportData className={styles.exportButton} id={id ?? -1} command={'periphery-type'} />
      <EditBtn idSelector={idSelector} />
      <DeleteBtn sendDelete={sendDelete} idSelector={idSelector} />
    </div>
  )
}

const countSelector = createSelector(getKnownEntities, p => (p || []).length)
const List = (
  props: Omit<
    GenericListProps<PeripheryType, RootState, PeripheryItemProps>,
    'count' | 'selectorFactory'
  >
) => {
  const count = usePTSelector(countSelector)
  return (
    <GenericList<PeripheryType, RootState, PeripheryItemProps>
      {...props}
      count={count}
      selectorFactory={peripherySelector}
    />
  )
}

export const InnerList = () => {
  const send = useSendCommand()
  const sendDelete = (id: number) => send('delete-periphery-type', id)
  return (
    <InventoryPage<RootState>
      styles={styles}
      getIsLoading={getIsLoading}
      getDataCommands={['get-periphery-types']}
      title={'List of periphery types'}
      addEntityText={'Add new periphery type'}
    >
      <List
        containerClassName={styles.list}
        sendDelete={sendDelete}
        Item={PeripheryItem}
        listConfigCss={{
          columns: 3,
          itemMaxHeight: '200px'
        }}
      />
    </InventoryPage>
  )
}
