import React from 'react'
import { getKnownTypes } from './selectors'
import { createList, type ItemProps, type ListItem } from '../../../utils/list'
import { connect } from 'react-redux'
import styles from './list.scss'
import { editType, startNewType } from './actions'
import { redirect } from 'react-router'
import type { RootState, PeripheryDirection } from './types'
import classNames from 'classnames'
import Button from '@mui/material/Button'
import { editButton } from '../form'

const mapName =
  () =>
  (state: RootState, { key }: ItemProps) => ({
    name: getKnownTypes(state)[key].name
  })

const PeripheryName = connect(mapName)(({ name }: { name: string }) => (
  <span>{name}</span>
))

type PictureArg = {
  picture: string
  name: string
}

const mapPicture =
  () =>
  (state: RootState, { key }: ItemProps) => ({
    picture: getKnownTypes(state)[key].picture || '',
    name: getKnownTypes(state)[key].name
  })

const PeripheryPicture = connect(mapPicture)(({ picture, name }: PictureArg) => (
  <img src={picture} alt={`Periphery ${name}`} />
))

const EditButton = editButton(styles.edit, editType, getKnownTypes)

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
    <PeripheryPicture key={key} />
    <PeripheryName key={key} />
    <PeripheryUnits key={key} />
    <PeripheryDirection key={key} />
    <PeripheryDescription key={key} />
    <EditButton key={key} />
  </div>
)

const AddButton = connect(null, (dispatch) => ({
  onClick: () => {
    dispatch(startNewType())
    redirect('new')
  }
}))(({ onClick }: { onClick: () => void }) => (
  <Button className={styles.add} onClick={onClick}>
    Add Periphery Type
  </Button>
))

const PList = createList(getKnownTypes)(PeripheryItem)

export const PeripheryTypesList = () => (
  <div className={styles.container}>
    <h1>Periphery Types</h1>
    <AddButton />
    <div className={styles.list}>
      <PList />
    </div>
  </div>
)
