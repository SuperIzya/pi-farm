import React from 'react'
import { getIsLoading, getKnownEntities, getKnownPeripheryKeys } from './selectors'
import { createList, type ItemProps, type ListItem } from '../../../utils/list-mixin'
import { connect } from 'react-redux'
import * as rawStyles from './list.scss'
import type { RootState } from './types'
import classNames from 'classnames'
import { AddButton, DeleteButton, EditButton } from '../form-mixin'
import type { PeripheryDirection } from '../../../types'
import { WaitLoading } from '../../../utils/wait-loading'
import { useSendCommand } from '../../../client'
import { setLoading } from './actions'
import { Text } from '../../../utils/text'

type Styles = typeof rawStyles
type PListStyles = { [key in keyof PeripheryDirection]: string } & Styles
const styles = rawStyles as PListStyles

const mapName =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    name: getKnownEntities(state)[itemKey].name
  })

const PeripheryName = connect(mapName)(({ name }: { name: string }) => (
  <span className={styles.name}>{name}</span>
))

type ImageProps = {
  image: string
  name: string
}

const mapPicture =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    image: getKnownEntities(state)[itemKey].image || '',
    name: getKnownEntities(state)[itemKey].name
  })

const PeripheryImage = connect(mapPicture)(({ image, name }: ImageProps) => (
  <img src={image} alt={`Periphery ${name}`} />
))

const directionComponent = ({ direction }: { direction: PeripheryDirection }) => (
  // @ts-ignore
  <span className={classNames(styles.direction, styles[direction])}>{direction}</span>
)

const mapDirection =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    direction: getKnownEntities(state)[itemKey].direction
  })

const PeripheryDirection = connect(mapDirection)(directionComponent)

const mapDescription =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    description: getKnownEntities(state)[itemKey].description
  })

const PeripheryDescription = connect(mapDescription)(
  ({ description }: { description: string }) => (
    <Text className={styles.description} text={description} />
  )
)

const mapUnits =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    units: getKnownEntities(state)[itemKey].units
  })

const PeripheryUnits = connect(mapUnits)(({ units }: { units: string }) => (
  <span className={styles.units}>{units}</span>
))

type PeripheryItemProps = {
  sendDelete: (id: number) => void
}

const PeripheryItem: ListItem<PeripheryItemProps> = ({ itemKey, sendDelete }) => (
  <div className={styles.item}>
    <PeripheryName itemKey={itemKey} />
    <PeripheryUnits itemKey={itemKey} />
    <PeripheryImage itemKey={itemKey} />
    <PeripheryDirection itemKey={itemKey} />
    <PeripheryDescription itemKey={itemKey} />
    <EditButton
      itemKey={itemKey}
      className={styles.editButton}
      objectsExtractor={getKnownEntities}
    />
    <DeleteButton
      itemKey={itemKey}
      className={styles.deleteButton}
      objectsExtractor={getKnownEntities}
      onDelete={sendDelete}
      isLoading={setLoading}
      itemName={'periphery type'}
    />
  </div>
)

const PList = createList(getKnownPeripheryKeys)(PeripheryItem)

export const PeripheryTypesList = () => {
  const send = useSendCommand()
  const sendDelete = (id: number) => send('delete-periphery-type', id)
  return (
    <div className={styles.container}>
      <h1>List of periphery types</h1>
      <AddButton className={styles.add} text={'Add new periphery type'} />

      <WaitLoading isLoadingSelector={getIsLoading}>
        <PList containerClassName={styles.list} sendDelete={sendDelete} />
      </WaitLoading>
    </div>
  )
}
