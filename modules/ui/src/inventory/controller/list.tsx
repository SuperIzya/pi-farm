import React from 'react'
import { useSendCommand } from '../../client'
import { Controller, ControllerType, IdType } from '../../types'
import * as styles from './list.scss'
import { AddButton, ClassName, DeleteButton, EditButton } from '../form-mixin'
import { WaitLoading } from '../../utils/wait-loading'
import { getIsLoading, getKnownEntities } from './selectors'
import { getKnownEntities as knownControllerTypes } from '../controller-types/selectors'
import {
  GenericList,
  GenericListProps,
  getListKey,
  ListItem
} from '../../utils/list-mixin'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'
import { Text } from '../../utils/text'
import { PeripheryList } from '../controller-types/periphery-list'
import { setLoading } from './actions'

const controllerSelector = <T,>(f: (c: Controller) => T) =>
  createSelector([getKnownEntities, getListKey], (controllers, itemKey) =>
    f(controllers[itemKey])
  )

const controllerIdSelector = () => controllerSelector(({ id }) => ({ id }))

const controllerTypeIdSelector = () => controllerSelector(({ typeId }) => ({ typeId }))

const controllerTypeSelector = <T,>(f: (c: ControllerType) => T) =>
  createSelector(
    [controllerTypeIdSelector(), knownControllerTypes],
    ({ typeId }, types) => f(types.find(({ id }) => typeId === id)!)
  )

const TextComponent = ({ text, className }: { text: string } & ClassName) => (
  <Text className={className} text={text} />
)
const Name = connect(() => controllerSelector(({ name: text }) => ({ text })))(
  TextComponent
)

const TypeName = connect(() => controllerTypeSelector(({ name: text }) => ({ text })))(
  TextComponent
)

const Description = connect(() =>
  controllerSelector(({ description: text }) => ({ text }))
)(TextComponent)

const TypeDescription = connect(() =>
  controllerTypeSelector(({ description: text }) => ({ text }))
)(TextComponent)

type ControllerItemProps = {
  sendDelete: (id: IdType) => void
}

const connectId = connect(controllerIdSelector)

const EditBtn = connectId(({ id }: { id: IdType }) => (
  <EditButton className={styles.editButton} id={id} />
))

const DeleteBtn = connectId(
  ({ id, sendDelete }: { id: IdType } & ControllerItemProps) => (
    <DeleteButton
      id={id}
      className={styles.deleteButton}
      onDelete={sendDelete}
      isLoading={setLoading}
      itemName={'periphery type'}
    />
  )
)

const Item: ListItem<ControllerItemProps> = ({ itemKey, sendDelete }) => (
  <div className={styles.item}>
    <Name itemKey={itemKey} className={styles.name} />
    <Description itemKey={itemKey} className={styles.description} />
    <TypeName itemKey={itemKey} className={styles.typeName} />
    <TypeDescription itemKey={itemKey} className={styles.typeDescription} />
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
    <EditBtn itemKey={itemKey} />
    <DeleteBtn sendDelete={sendDelete} itemKey={itemKey} />
  </div>
)

const mapCount = createSelector([getKnownEntities], ({ length }) => ({ count: length }))
const List = connect(mapCount)((p: GenericListProps<ControllerItemProps>) => (
  <GenericList {...p} />
))

export const InnerList = () => {
  const send = useSendCommand()
  const sendDelete = (id: IdType) => send('delete-controller', id)
  return (
    <div className={styles.container}>
      <h1>List of controllers</h1>
      <AddButton className={styles.add} text={'Add new controller'} />

      <WaitLoading isLoadingSelector={getIsLoading}>
        <List containerClassName={styles.list} sendDelete={sendDelete} Item={Item} />
      </WaitLoading>
    </div>
  )
}
