import React from 'react'
import * as styles from './list.scss'
import { createList, ItemProps } from '../../../utils/list-mixin'
import { getKnownTypes, getPeripheryImage, getPeripheryName } from './selectors'
import { connect } from 'react-redux'
import type { RootState } from './types'
import { EditButton, AddButton } from '../form-mixin'

type PeripheryIndex = { idx: number }
type PeripheryItemProps = ItemProps<PeripheryIndex>

const mapName =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    name: getKnownTypes(state)[itemKey].name
  })

const Name = connect(mapName)(({ name }: { name: string }) => (
  <span className={styles.name}>{name}</span>
))

const mapDescription =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    description: getKnownTypes(state)[itemKey].description || ''
  })

const Description = connect(mapDescription)(
  ({ description }: { description: string }) => (
    <span className={styles.description}>{description}</span>
  )
)

const mapSchema =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    schema: getKnownTypes(state)[itemKey].schema
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
  (state: RootState, { itemKey }: ItemProps) => ({
    code: getKnownTypes(state)[itemKey].code
  })
const Code = connect(mapCode)(({ code }: { code: string }) => (
  <span className={styles.code}>{code}</span>
))

const PeripheryPicture = connect(() => {
  const selector = getPeripheryImage()
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
})(({ name }: { name: string }) => <span className={styles.peripheryName}>{name}</span>)

const PeripheryItem = ({ itemKey, idx }: PeripheryItemProps) => (
  <div className={styles.item}>
    <PeripheryName itemKey={itemKey} idx={idx} />
    <PeripheryPicture itemKey={itemKey} idx={idx} />
  </div>
)

const peripheryCountSelector = (state: RootState, { idx }: { idx: number }) =>
  Object.keys(getKnownTypes(state)[idx].peripheries)

const listCreator = createList(peripheryCountSelector)

const PeripheryList = listCreator<PeripheryIndex>(PeripheryItem)

const Item = ({ itemKey }: ItemProps) => (
  <div className={styles.item}>
    <Name itemKey={itemKey} />
    <Description itemKey={itemKey} />
    <div className={styles.plist}>
      <PeripheryList idx={itemKey} />
    </div>
    <Code itemKey={itemKey} />
    <Schema itemKey={itemKey} />
    <EditButton
      itemKey={itemKey}
      className={styles.edit}
      objectsExtractor={getKnownTypes}
    />
  </div>
)

const PList = createList(getKnownTypes)(Item)

export const ControllerTypesList = () => {
  return (
    <div className={styles.container}>
      <h1>Controller Types List</h1>
      <AddButton className={styles.add} text={'Add controller type'} />
      <PList />
    </div>
  )
}
