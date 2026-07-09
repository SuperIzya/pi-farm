import React from 'react'
import { getIsLoading, getKnownEntities, usePTSelector } from './selectors'
import { GenericList, GenericListProps, ItemProps, type ListItem } from '../../utils/list-mixin'
import * as rawStyles from './list.scss'
import { AddButton, DeleteButton, EditButton } from '../form-mixin'
import type { FlowDirection } from '../../types'
import { WaitLoading } from '../../utils/wait-loading'
import { useSendCommand } from '../../client'
import { setLoading } from './actions'
import { Text } from '../../utils/text'
import { Guard } from './guard'
import { ConnectionsList } from './connections'
import { buildItemSelector } from '../store-mixin'
import {DescriptionIcon} from '../../utils/icons'

type Styles = typeof rawStyles
type PListStyles = { [key in keyof FlowDirection]: string } & Styles
const styles = rawStyles as PListStyles


const peripherySelector = buildItemSelector(getKnownEntities)

const PeripheryName = ({itemKey}: ItemProps) => {
  const name = usePTSelector(peripherySelector(itemKey, ({ name }) => name))
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

const PeripheryImage = ({itemKey}: ItemProps) => {
  const { image, name } = usePTSelector(peripherySelector(itemKey, periphery => ({
    image: periphery.image || '',
    name: periphery.name
  })))
  return <PImage image={image} name={name} />
}

const PeripheryDescription = ({itemKey}: ItemProps) => {
  const description = usePTSelector(peripherySelector(itemKey, ({ description }) => description))
  return <Text className={styles.description} text={description} icon={<DescriptionIcon />} />
}

const mapId = (itemKey: number) => peripherySelector(itemKey, ({ id }) => id)

const EditBtn = ({itemKey}: ItemProps) => {
  const id = usePTSelector(mapId(itemKey))
  return <EditButton id={id} className={styles.editButton} />
}

type PeripheryItemProps = {
  sendDelete: (id: number) => void
}

const DeleteBtn = ({itemKey, sendDelete}: ItemProps & PeripheryItemProps) => {
  const id = usePTSelector(mapId(itemKey))
  return (
    <DeleteButton
      id={id}
      className={styles.deleteButton}
      onDelete={sendDelete}
      isLoading={setLoading}
      itemName={'periphery type'}
    />
  )
}

const PeripheryItem: ListItem<PeripheryItemProps> = ({ itemKey, sendDelete }) => (
  <div className={styles.item}>
    <PeripheryName itemKey={itemKey} />
    <PeripheryImage itemKey={itemKey} />
    <div className={styles.connections}>
      <ConnectionsList itemKey={itemKey} />
    </div>
    <PeripheryDescription itemKey={itemKey} />
    <EditBtn itemKey={itemKey} />
    <DeleteBtn sendDelete={sendDelete} itemKey={itemKey} />
  </div>
)

const List = (props: Omit<GenericListProps<PeripheryItemProps>, 'count'>) => {
  const count = usePTSelector(s => (getKnownEntities(s) || []).length)
  return (
    <GenericList {...props} count={count} />
  )
}

export const InnerList = () => {
  const send = useSendCommand()
  const sendDelete = (id: number) => send('delete-periphery-type', id)
  return (
    <div className={styles.container}>
      <Guard />
      <h1>List of periphery types</h1>
      <AddButton className={styles.add} text={'Add new periphery type'} />

      <WaitLoading isLoadingSelector={getIsLoading}>
        <List
          containerClassName={styles.list}
          sendDelete={sendDelete}
          Item={PeripheryItem}
          listConfigCss={{
            columns: 3,
            itemMaxHeight: '200px',
          }}
        />
      </WaitLoading>
    </div>
  )
}
