import React from 'react'
import styles from './list.scss'
import { createList, ItemProps } from '../../../utils/list'
import { getKnownTypes, getPeripheryPicture, getPeripheryName } from './selectors'
import { connect } from 'react-redux'
import type { RootState } from './types'
import { editType, startNewType } from './actions'
import { redirect } from 'react-router'
import Button from '@mui/material/Button'
import { editButton } from '../form'

type PeripheryIndex = { idx: number }
type PeripheryItemProps = ItemProps<PeripheryIndex>

const mapName =
  () =>
  (state: RootState, { key }: ItemProps) => ({
    name: getKnownTypes(state)[key].name
  })

const Name = connect(mapName)(({ name }: { name: string }) => (
  <span className={styles.name}>{name}</span>
))

const mapDescription =
  () =>
  (state: RootState, { key }: ItemProps) => ({
    description: getKnownTypes(state)[key].description || ''
  })

const Description = connect(mapDescription)(
  ({ description }: { description: string }) => (
    <span className={styles.description}>{description}</span>
  )
)

const mapSchema =
  () =>
  (state: RootState, { key }: ItemProps) => ({
    schema: getKnownTypes(state)[key].schema
  })

const Schema = connect(mapSchema)(
  ({ schema }: { schema: string | undefined }) =>
    schema && (
      <a href={schema} target="_blank" rel="noreferrer">
        Schema
      </a>
    )
)

const mapCode =
  () =>
  (state: RootState, { key }: ItemProps) => ({
    code: getKnownTypes(state)[key].code
  })
const Code = connect(mapCode)(({ code }: { code: string }) => (
  <span className={styles.code}>{code}</span>
))

const PeripheryPicture = connect(() => {
  const selector = getPeripheryPicture()
  return (state: RootState, props: PeripheryItemProps) => ({
    picture: selector(state, props)
  })
})(
  ({ picture }: { picture: string | null }) =>
    picture && <img className={styles.picture} src={picture} />
)

const PeripheryName = connect(() => {
  const selector = getPeripheryName()
  return (state: RootState, props: PeripheryItemProps) => ({
    name: selector(state, props)
  })
})(({ name }: { name: string }) => <span className={styles.name}>{name}</span>)

const PeripheryItem = ({ key, idx }: PeripheryItemProps) => (
  <div className={styles.item}>
    <PeripheryName key={key} idx={idx} />
    <PeripheryPicture key={key} idx={idx} />
  </div>
)

const peripheryCountSelector = (state: RootState, { idx }: { idx: number }) =>
  Object.keys(getKnownTypes(state)[idx].peripheries)

const listCreator = createList(peripheryCountSelector)

const PeripheryList = listCreator<PeripheryIndex>(PeripheryItem)

const EditButton = editButton(styles.edit, editType, getKnownTypes)

const Item = ({ key }: ItemProps) => (
  <div className={styles.item}>
    <Name key={key} />
    <Description key={key} />
    <div className={styles.plist}>
      <PeripheryList idx={key} />
    </div>
    <Code key={key} />
    <Schema key={key} />
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
const PList = createList(getKnownTypes)(Item)

export const ControllerTypesList = () => (
  <div className={styles.container}>
    <h1>Controller Types List</h1>
    <AddButton />
    <PList />
  </div>
)
