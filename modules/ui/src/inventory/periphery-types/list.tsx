import React from 'react'
import { getIsLoading, getKnownEntities } from './selectors'
import {
  GenericList,
  GenericListProps,
  getListKey,
  type ListItem
} from '../../utils/list-mixin'
import { connect } from 'react-redux'
import * as rawStyles from './list.scss'
import { AddButton, DeleteButton, EditButton } from '../form-mixin'
import type { IdType, PeripheryDirection, PeripheryType } from '../../types'
import { WaitLoading } from '../../utils/wait-loading'
import { useSendCommand } from '../../client'
import { setLoading } from './actions'
import { Text } from '../../utils/text'
import { Direction } from './direction'
import { createSelector } from 'reselect'
import { Guard } from './guard'

type Styles = typeof rawStyles
type PListStyles = { [key in keyof PeripheryDirection]: string } & Styles
const styles = rawStyles as PListStyles

const peripherySelector = <T,>(f: (p: PeripheryType) => T) =>
  createSelector([getKnownEntities, getListKey], (entities, itemKey) =>
    f(entities[itemKey])
  )

const mapName = () => peripherySelector(({ name }) => ({ name }))

const PeripheryName = connect(mapName)(({ name }: { name: string }) => (
  <Text className={styles.name} text={name} />
))

type ImageProps = {
  image: string
  name: string
}

const mapPicture = () =>
  peripherySelector((periphery) => ({
    image: periphery.image || '',
    name: periphery.name
  }))

const PeripheryImage = connect(mapPicture)(({ image, name }: ImageProps) => (
  <img src={image} alt={`Periphery ${name}`} />
))

const directionComponent = ({ direction }: { direction: PeripheryDirection }) => (
  <Direction direction={direction} className={styles.direction} />
)

const mapDirection = () => peripherySelector(({ direction }) => ({ direction }))

const PeripheryDirection = connect(mapDirection)(directionComponent)

const mapDescription = () => peripherySelector(({ description }) => ({ description }))

const PeripheryDescription = connect(mapDescription)(
  ({ description }: { description: string }) => (
    <Text className={styles.description} text={description} />
  )
)

const mapUnits = () => peripherySelector(({ units }) => ({ units }))

const PeripheryUnits = connect(mapUnits)(({ units }: { units: string }) => (
  <span className={styles.units}>{units}</span>
))

const mapId = () => peripherySelector(({ id }) => ({ id }))

const EditBtn = connect(mapId)(({ id }: { id: IdType }) => (
  <EditButton id={id} className={styles.editButton} />
))

type PeripheryItemProps = {
  sendDelete: (id: number) => void
}

const DeleteBtn = connect(mapId)(
  ({ id, sendDelete }: { id: IdType } & PeripheryItemProps) => (
    <DeleteButton
      id={id}
      className={styles.deleteButton}
      onDelete={sendDelete}
      isLoading={setLoading}
      itemName={'periphery type'}
    />
  )
)

const PeripheryItem: ListItem<PeripheryItemProps> = ({ itemKey, sendDelete }) => (
  <div className={styles.item}>
    <PeripheryName itemKey={itemKey} />
    <PeripheryUnits itemKey={itemKey} />
    <PeripheryImage itemKey={itemKey} />
    <PeripheryDirection itemKey={itemKey} />
    <PeripheryDescription itemKey={itemKey} />
    <EditBtn itemKey={itemKey} />
    <DeleteBtn sendDelete={sendDelete} itemKey={itemKey} />
  </div>
)

const mapCount = createSelector([getKnownEntities], ({ length }) => ({
  count: length
}))

const List = connect(mapCount)((props: GenericListProps<PeripheryItemProps>) => (
  <GenericList {...props} />
))

export const PeripheryTypesList = () => {
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
        />
      </WaitLoading>
    </div>
  )
}
