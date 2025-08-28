import React from 'react'
import * as styles from './list.scss'
import {
  GenericList,
  GenericListProps,
  getListKey,
  ListItem
} from '../../utils/list-mixin'
import { getKnownEntities, getIsLoading } from './selectors'
import { connect } from 'react-redux'
import { EditButton, AddButton, DeleteButton } from '../form-mixin'
import { setLoading } from './actions'
import { useSendCommand } from '../../client'
import { WaitLoading } from '../../utils/wait-loading'
import { Text } from '../../utils/text'
import { ControllerType, IdType } from '../../types'
import { PeripheryList } from './periphery-list'
import { createSelector } from 'reselect'
import { Guard } from '../periphery-types/guard'

const controllerTypeSelector = <T,>(f: (c: ControllerType) => T) =>
  createSelector([getKnownEntities, getListKey], (entities, itemKey) =>
    f(entities[itemKey])
  )

const mapName = () => controllerTypeSelector(({ name }) => ({ name }))

const mapDescription = () =>
  controllerTypeSelector((controller) => ({
    description: controller.description || ''
  }))

const mapSchema = () => controllerTypeSelector(({ schema }) => ({ schema }))

const mapCode = () => controllerTypeSelector(({ code }) => ({ code }))

type ControllerItemProps = {
  sendDelete: (id: number) => void
}
const Name = connect(mapName)(({ name }: { name: string }) => (
  <Text className={styles.name} text={name} />
))

const Description = connect(mapDescription)(
  ({ description }: { description: string }) => (
    <Text className={styles.description} text={description} />
  )
)

const Schema = connect(mapSchema)(
  ({ schema }: { schema: string | undefined }) =>
    schema && (
      <a href={schema} target="_blank" rel="noreferrer">
        Schema
      </a>
    )
)

const Code = connect(mapCode)(({ code }: { code: string }) => (
  <span className={styles.code}>{code}</span>
))

const mapId = connect(() => controllerTypeSelector(({ id }) => ({ id })))

const EditBtn = mapId(({ id }: { id: IdType }) => (
  <EditButton id={id} className={styles.editButton} />
))

const DeleteBtn = mapId(({ id, sendDelete }: { id: IdType } & ControllerItemProps) => (
  <DeleteButton
    id={id}
    className={styles.deleteButton}
    onDelete={sendDelete}
    isLoading={setLoading}
    itemName={'periphery type'}
  />
))

const Item: ListItem<ControllerItemProps> = ({ itemKey, sendDelete }) => (
  <div className={styles.item}>
    <Name itemKey={itemKey} />
    <Description itemKey={itemKey} />
    <Code itemKey={itemKey} />
    <Schema itemKey={itemKey} />
    <PeripheryList
      idx={itemKey}
      containerClassName={styles.plist}
      listConfigCss={{
        columns: 3,
        maxWidth: '100%',
        columnMin: '50px',
        columnMax: '75px'
      }}
    />
    <EditBtn itemKey={itemKey} />
    <DeleteBtn sendDelete={sendDelete} itemKey={itemKey} />
  </div>
)

const countSelector = createSelector([getKnownEntities], ({ length }) => ({
  count: length
}))

const List = connect(countSelector)((props: GenericListProps<ControllerItemProps>) => (
  <GenericList {...props} />
))

export const ControllerTypesList = () => {
  const send = useSendCommand()
  const sendDelete = (id: IdType) => send('delete-controller-type', id)
  return (
    <div className={styles.container}>
      <Guard />
      <h1>List of controller types</h1>
      <AddButton className={styles.add} text={'Add new controller type'} />

      <WaitLoading isLoadingSelector={getIsLoading}>
        <List containerClassName={styles.list} sendDelete={sendDelete} Item={Item} />
      </WaitLoading>
    </div>
  )
}
