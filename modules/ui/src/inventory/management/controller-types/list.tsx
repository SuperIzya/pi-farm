import React from 'react'
import styles from './list.scss'
import { createList, ItemArg } from '../../../utils/list'
import { getKnownTypes, getPeripheryPicture, getPeripheryName } from './selectors'
import { connect } from 'react-redux'
import type { RootState } from './types'
import { editType, startNewType } from './actions'
import { redirect } from 'react-router'
import Button from '@mui/material/Button'
import { editButton } from '../form'

const mapName =
  () =>
  (state: RootState, { index }: ItemArg) => ({
    name: getKnownTypes(state)[index].name
  })

const Name = connect(mapName)(({ name }: { name: string }) => (
  <span className={styles.name}>{name}</span>
))

const mapDescription =
  () =>
  (state: RootState, { index }: ItemArg) => ({
    description: getKnownTypes(state)[index].description || ''
  })

const Description = connect(mapDescription)(
  ({ description }: { description: string }) => (
    <span className={styles.description}>{description}</span>
  )
)

const mapSchema =
  () =>
  (state: RootState, { index }: ItemArg) => ({
    schema: getKnownTypes(state)[index].schema
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
  (state: RootState, { index }: ItemArg) => ({
    code: getKnownTypes(state)[index].code
  })
const Code = connect(mapCode)(({ code }: { code: string }) => (
  <span className={styles.code}>{code}</span>
))

const PeripheryPicture = connect(() => {
  const selector = getPeripheryPicture()
  return (state: RootState, props: ItemArg & { idx: number }) => ({
    picture: selector(state, props)
  })
})(
  ({ picture }: { picture: string | null }) =>
    picture && <img className={styles.picture} src={picture} />
)

const PeripheryName = connect(() => {
  const selector = getPeripheryName()
  return (state: RootState, props: ItemArg & { idx: number }) => ({
    name: selector(state, props)
  })
})(({ name }: { name: string }) => <span className={styles.name}>{name}</span>)

const PeripheryItem = ({ index, key }: { index: number; key: number }) => (
  <div className={styles.item}>
    <PeripheryName index={index} idx={key} />
    <PeripheryPicture index={index} idx={key} />
  </div>
)

const mapPeripheryCount =
  () =>
  (state: RootState, { index }: ItemArg) => ({
    count: getKnownTypes(state)[index].periphery.length
  })

const PeripheryList = connect(mapPeripheryCount)(
  ({ count, index }: { count: number; index: number }) => (
    <div className={styles.plist}>
      {Array.from(Array(count).keys()).map((i) => (
        <PeripheryItem index={index} key={i} />
      ))}
    </div>
  )
)

const EditButton = editButton(styles.edit, editType, getKnownTypes)

const Item = ({ index }: ItemArg) => (
  <div className={styles.item}>
    <Name index={index} />
    <Description index={index} />
    <PeripheryList index={index} />
    <Code index={index} />
    <Schema index={index} />
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

export const ControllerTypesList = () => (
  <div className={styles.container}>
    <h1>Controller Types List</h1>
    <AddButton />
    <PList item={Item} />
  </div>
)
