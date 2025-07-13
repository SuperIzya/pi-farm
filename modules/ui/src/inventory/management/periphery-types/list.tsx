import React from 'react'
import { getKnownTypes } from './selectors'
import { createList, type ItemArg } from '../../../utils/list'
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
  (state: RootState, { index }: ItemArg) => ({
    name: getKnownTypes(state)[index].name
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
  (state: RootState, { index }: ItemArg) => ({
    picture: getKnownTypes(state)[index].picture || '',
    name: getKnownTypes(state)[index].name
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
  (state: RootState, { index }: ItemArg) => ({
    direction: getKnownTypes(state)[index].direction
  })

const PeripheryDirection = connect(mapDirection)(directionComponent)

const mapDescription =
  () =>
  (state: RootState, { index }: ItemArg) => ({
    description: getKnownTypes(state)[index].description
  })

const PeripheryDescription = connect(mapDescription)(
  ({ description }: { description: string }) => (
    <span className={styles.description}>{description}</span>
  )
)

const mapUnits =
  () =>
  (state: RootState, { index }: ItemArg) => ({
    units: getKnownTypes(state)[index].units
  })

const PeripheryUnits = connect(mapUnits)(({ units }: { units: string }) => (
  <span className={styles.units}>{units}</span>
))

const PeripheryItem = ({ index }: ItemArg) => (
  <div className={styles.item}>
    <PeripheryPicture index={index} />
    <PeripheryName index={index} />
    <PeripheryUnits index={index} />
    <PeripheryDirection index={index} />
    <PeripheryDescription index={index} />
    <EditButton index={index} />
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

const PList = createList(getKnownTypes)

export const PeripheryTypesList = () => (
  <div className={styles.container}>
    <h1>Periphery Types</h1>
    <AddButton />
    <div className={styles.list}>
      <PList item={PeripheryItem} />
    </div>
  </div>
)
