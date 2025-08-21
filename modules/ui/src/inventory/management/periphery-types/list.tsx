import React from 'react'
import { getIsLoading, getKnownTypes } from './selectors'
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

type Styles = typeof rawStyles
type PListStyles = { [key in keyof PeripheryDirection]: string } & Styles
const styles = rawStyles as PListStyles

const mapName =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    name: getKnownTypes(state)[itemKey].name
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
    image: getKnownTypes(state)[itemKey].image || '',
    name: getKnownTypes(state)[itemKey].name
  })

const PeripheryImage = connect(mapPicture)(({ image, name }: ImageProps) => (
  <img src={image} alt={`Periphery ${name}`} />
))

const directionComponent = ({ direction }: { direction: PeripheryDirection }) => (
  <span className={classNames(styles.direction, styles[direction])}>{direction}</span>
)

const mapDirection =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    direction: getKnownTypes(state)[itemKey].direction
  })

const PeripheryDirection = connect(mapDirection)(directionComponent)

const mapDescription =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    description: getKnownTypes(state)[itemKey].description
  })

const PeripheryDescription = connect(mapDescription)(
  ({ description }: { description: string }) => (
    <span className={styles.description}>{description}</span>
  )
)

const mapUnits =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    units: getKnownTypes(state)[itemKey].units
  })

const PeripheryUnits = connect(mapUnits)(({ units }: { units: string }) => (
  <span className={styles.units}>{units}</span>
))

type PeripheryItemProps = {
  sendDelete: (id: number) => void
}

const PeripheryItem: ListItem<PeripheryItemProps> = ({ itemKey, sendDelete }) => (
  <div className={classNames(styles.listItem, styles.item)}>
    <PeripheryName itemKey={itemKey} />
    <PeripheryUnits itemKey={itemKey} />
    <PeripheryImage itemKey={itemKey} />
    <PeripheryDirection itemKey={itemKey} />
    <PeripheryDescription itemKey={itemKey} />
    <EditButton
      itemKey={itemKey}
      className={styles.editButton}
      objectsExtractor={getKnownTypes}
    />
    <DeleteButton
      itemKey={itemKey}
      className={styles.deleteButton}
      objectsExtractor={getKnownTypes}
      sendDelete={sendDelete}
      isLoading={setLoading}
      itemName={'periphery type'}
    />
  </div>
)

const PList = createList(getKnownTypes)(PeripheryItem)

export const PeripheryTypesList = () => {
  const send = useSendCommand()
  const sendDelete = (id: number) => send('delete-periphery-type', id)
  return (
    <div className={styles.container}>
      <h1>Periphery Types</h1>
      <AddButton className={styles.add} text={'Add periphery type'} />

      <WaitLoading isLoadingSelector={getIsLoading}>
        <PList
          containerClassName={styles.list}
          sendDelete={sendDelete}
          listConfigCss={{
            columns: 3,
            gridGap: '5px',
            maxWidth: '50%'
          }}
        />
      </WaitLoading>
    </div>
  )
}
