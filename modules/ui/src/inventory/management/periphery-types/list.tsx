import React from 'react'
import { getKnownTypes } from './selectors'
import { createList, type ItemProps, type ListItem } from '../../../utils/list'
import { connect } from 'react-redux'
import * as styles from './list.scss'
import type { RootState } from './types'
import classNames from 'classnames'
import { AddButton, EditButton } from '../form'
import type { PeripheryDirection } from '../../../types'

const mapName =
  () =>
  (state: RootState, { key }: ItemProps) => ({
    name: getKnownTypes(state)[key].name
  })

const PeripheryName = connect(mapName)(({ name }: { name: string }) => (
  <span>{name}</span>
))

type ImageProps = {
  image: string
  name: string
}

const mapPicture =
  () =>
  (state: RootState, { key }: ItemProps) => ({
    image: getKnownTypes(state)[key].image || '',
    name: getKnownTypes(state)[key].name
  })

const PeripheryImage = connect(mapPicture)(({ image, name }: ImageProps) => (
  <img src={image} alt={`Periphery ${name}`} />
))

const directionComponent = ({ direction }: { direction: PeripheryDirection }) => (
  <span className={classNames(styles.direction, styles[direction])}>{direction}</span>
)

const mapDirection =
  () =>
  (state: RootState, { key }: ItemProps) => ({
    direction: getKnownTypes(state)[key].direction
  })

const PeripheryDirection = connect(mapDirection)(directionComponent)

const mapDescription =
  () =>
  (state: RootState, { key }: ItemProps) => ({
    description: getKnownTypes(state)[key].description
  })

const PeripheryDescription = connect(mapDescription)(
  ({ description }: { description: string }) => (
    <span className={styles.description}>{description}</span>
  )
)

const mapUnits =
  () =>
  (state: RootState, { key }: ItemProps) => ({
    units: getKnownTypes(state)[key].units
  })

const PeripheryUnits = connect(mapUnits)(({ units }: { units: string }) => (
  <span className={styles.units}>{units}</span>
))

const PeripheryItem: ListItem = ({ key }: ItemProps) => (
  <div className={styles.item}>
    <PeripheryImage key={key} />
    <PeripheryName key={key} />
    <PeripheryUnits key={key} />
    <PeripheryDirection key={key} />
    <PeripheryDescription key={key} />
    <EditButton key={key} className={styles.edit} objectsExtractor={getKnownTypes} />
  </div>
)

const PList = createList(getKnownTypes)(PeripheryItem)

export const PeripheryTypesList = () => {
  return (
    <div className={styles.container}>
      <h1>Periphery Types</h1>
      <AddButton className={styles.add} text={'Add periphery type'} />
      <div className={styles.list}>
        <PList />
      </div>
    </div>
  )
}
