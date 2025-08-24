import React from 'react'
import * as styles from './list.scss'
import { CountSelector, createList, ItemProps, ListItem } from '../../../utils/list-mixin'
import {
  getKnownEntities,
  getPeripheryImage,
  getPeripheryKey,
  getPeripheryName,
  sortPeripheriesKeys,
  getIsLoading
} from './selectors'
import { connect } from 'react-redux'
import type { RootState } from './types'
import { EditButton, AddButton, DeleteButton } from '../form-mixin'
import { setLoading } from './actions'
import { useSendCommand } from '../../../client'
import { WaitLoading } from '../../../utils/wait-loading'
import { Text } from '../../../utils/text'

type PeripheryIndex = { idx: number }
type PeripheryItemProps = ItemProps<PeripheryIndex>

const mapName =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    name: getKnownEntities(state)[itemKey].name
  })

const Name = connect(mapName)(({ name }: { name: string }) => (
  <span className={styles.name}>{name}</span>
))

const mapDescription =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    description: getKnownEntities(state)[itemKey].description || ''
  })

const Description = connect(mapDescription)(
  ({ description }: { description: string }) => (
    <Text className={styles.description} text={description} />
  )
)

const mapSchema =
  () =>
  (state: RootState, { itemKey }: ItemProps) => ({
    schema: getKnownEntities(state)[itemKey].schema
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
    code: getKnownEntities(state)[itemKey].code
  })
const Code = connect(mapCode)(({ code }: { code: string }) => (
  <span className={styles.code}>{code}</span>
))

const PeripheryKey = connect(() => {
  const selector = getPeripheryKey()
  return (state: RootState, props: PeripheryItemProps) => ({
    keyName: selector(state, props)
  })
})(({ keyName }: { keyName: string }) => (
  <div className={styles.peripheryKey}>
    <span>{keyName}</span>
  </div>
))

const PeripheryImage = connect(() => {
  const imgSelector = getPeripheryImage()
  const nameSelector = getPeripheryName()
  return (state: RootState, props: PeripheryItemProps) => ({
    image: imgSelector(state, props),
    name: nameSelector(state, props)
  })
})(
  ({ image, name }: { image: string | null; name: string }) =>
    image && <img className={styles.peripheryImage} src={image} alt={name} />
)

const PeripheryName = connect(() => {
  const selector = getPeripheryName()
  return (state: RootState, props: PeripheryItemProps) => ({
    name: selector(state, props)
  })
})(({ name }: { name: string }) => <span className={styles.peripheryName}>{name}</span>)

const PeripheryItem = ({ itemKey, idx }: PeripheryItemProps) => (
  <div className={styles.peripheryItem}>
    <PeripheryKey itemKey={itemKey} idx={idx} />
    <PeripheryName itemKey={itemKey} idx={idx} />
    <PeripheryImage itemKey={itemKey} idx={idx} />
  </div>
)

const peripheryCountSelector: CountSelector<RootState, string, { idx: number }> = (
  state: RootState,
  { idx }: { idx: number }
) => sortPeripheriesKeys(Object.keys(getKnownEntities(state)[idx].peripheries))

const listCreator = createList(peripheryCountSelector)

const PeripheryList = listCreator<PeripheryIndex>(PeripheryItem)

type ControllerItemProps = {
  sendDelete: (id: number) => void
}
const Item: ListItem<ControllerItemProps> = ({ itemKey, sendDelete }) => (
  <div className={styles.item}>
    <Name itemKey={itemKey} />
    <Description itemKey={itemKey} />
    <PeripheryList
      containerClassName={styles.plist}
      idx={itemKey}
      listConfigCss={{
        columns: 'auto-fill',
        maxWidth: '100%',
        columnMin: '50px',
        columnMax: '75px'
      }}
    />
    <Code itemKey={itemKey} />
    <Schema itemKey={itemKey} />
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

const PList = createList(getKnownEntities)(Item)

export const ControllerTypesList = () => {
  const send = useSendCommand()
  const sendDelete = (id: number) => send('delete-controller-type', id)
  return (
    <div className={styles.container}>
      <h1>Controller Types List</h1>
      <AddButton className={styles.add} text={'Add controller type'} />

      <WaitLoading isLoadingSelector={getIsLoading}>
        <PList containerClassName={styles.list} sendDelete={sendDelete} />
      </WaitLoading>
    </div>
  )
}
