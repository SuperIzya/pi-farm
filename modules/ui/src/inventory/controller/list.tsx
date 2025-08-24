import React from 'react'
import { useSendCommand } from '../../client'
import { IdType } from '../../types'
import * as styles from './list.scss'
import { AddButton, DeleteButton, EditButton } from '../management/form-mixin'
import { WaitLoading } from '../../utils/wait-loading'
import { getControllerKeys, getIsLoading, getKnownEntities } from './selectors'
import { getKnownEntities as knownControllerTypes } from '../management/controller-types/selectors'
import { createList, getListKey, ItemProps, ListItem } from '../../utils/list-mixin'
import { connect } from 'react-redux'
import { RootState } from './types'
import { createSelector } from 'reselect'
import { Text } from '../../utils/text'
import { PeripheryList } from '../management/controller-types/periphery-list'
import { setLoading } from './actions'

const controllerSelector = () =>
  createSelector(
    [getKnownEntities, getListKey],
    (controllers, key) => controllers[key].typeId
  )

const connectName = connect(() => {
  const selector = controllerSelector()
  return (state: RootState, props: ItemProps) => ({
    name: knownControllerTypes(state)[selector(state, props)].name
  })
})

const Name = connectName(({ name }: { name: string }) => (
  <span className={styles.name}>{name}</span>
))

const connectDescription = connect(() => {
  const selector = controllerSelector()
  return (state: RootState, props: ItemProps) => ({
    description: knownControllerTypes(state)[selector(state, props)].description || ''
  })
})
const Description = connectDescription(({ description }: { description: string }) => (
  <Text className={styles.description} text={description} />
))

type ControllerItemProps = {
  sendDelete: (id: IdType) => void
}

const Item: ListItem<ControllerItemProps> = ({ itemKey, sendDelete }) => (
  <div className={styles.item}>
    <div className={styles.id}>{itemKey}</div>
    <Name itemKey={itemKey} />
    <Description itemKey={itemKey} />
    <PeripheryList
      containerClassName={styles.plist}
      idx={itemKey}
      listConfigCss={{
        columns: 3,
        maxWidth: '100%',
        columnMin: '50px',
        columnMax: '75px'
      }}
    />
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

const CList = createList(getControllerKeys)(Item)

export const List = () => {
  const send = useSendCommand()
  const sendDelete = (id: IdType) => send('delete-controller', id)
  return (
    <div className={styles.container}>
      <h1>List of controllers</h1>
      <AddButton className={styles.add} text={'Add new controller'} />

      <WaitLoading isLoadingSelector={getIsLoading}>
        <CList containerClassName={styles.list} sendDelete={sendDelete} />
      </WaitLoading>
    </div>
  )
}
