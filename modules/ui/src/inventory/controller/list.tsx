import React from 'react'
import type { ControllerType, IdType } from '../../types'
import { useSendCommand } from '../../client'
import * as styles from './list.scss'
import { AddButton, ClassName, DeleteButton, EditButton } from '../form-mixin'
import { WaitLoading } from '../../utils/wait-loading'
import { getIsLoading, getKnownEntities, useCtlSelector } from './selectors'
import { getKnownEntities as knownControllerTypes } from '../controller-types/selectors'
import type { RootState as CTRootState } from '../controller-types/types'
import {
  GenericList,
  type GenericListProps,
  type ItemProps,
  ListItem
} from '../../utils/list-mixin'
import { Text } from '../../utils/text'
import { PeripheryList } from '../controller-types/periphery-list'
import { setLoading } from './actions'
import { buildItemSelector } from '../store-mixin'
import type { RootState } from './types'
import { DescriptionIcon, TypeIcon } from '../../utils/icons'

const controllerSelector = buildItemSelector(getKnownEntities)

const controllerTypeSelector =
  <T,>(itemKey: number, f: (c: ControllerType) => T) =>
  (state: CTRootState & RootState) => {
    const typeId = getKnownEntities(state)[itemKey].typeId
    const tpe = knownControllerTypes(state).find(({ id }) => id === typeId)
    return f(tpe!)
  }

const Name = ({ itemKey, className }: ItemProps & ClassName) => {
  const name = useCtlSelector(controllerSelector(itemKey, ({ name }) => name))
  return <Text className={className} text={name} />
}

const TypeName = ({ itemKey, className }: ItemProps & ClassName) => {
  const text = useCtlSelector(controllerTypeSelector(itemKey, ({ name }) => name))
  return <Text className={className} text={text} title='Type' />
}

const Description = ({ itemKey, className }: ItemProps & ClassName) => {
  const description = useCtlSelector(controllerSelector(itemKey, ({ description }) => description))
  return <Text className={className} text={description} icon={<DescriptionIcon />} />
}

const TypeDescription = ({ itemKey, className }: ItemProps & ClassName) => {
  const text = useCtlSelector(controllerTypeSelector(itemKey, ({ description }) => description))
  return <Text className={className} text={text} icon={<TypeIcon />} />
}

type ControllerItemProps = {
  sendDelete: (id: IdType) => void
}
const controllerIdSelector = (itemKey: number) => controllerSelector(itemKey, ({ id }) => id)

const EditBtn = ({ itemKey }: ItemProps) => {
  const id = useCtlSelector(controllerIdSelector(itemKey))
  return <EditButton className={styles.editButton} id={id} />
}

const DeleteBtn = ({ itemKey, sendDelete }: ItemProps & ControllerItemProps) => {
  const id = useCtlSelector(controllerIdSelector(itemKey))
  return (
    <DeleteButton
      id={id}
      className={styles.deleteButton}
      onDelete={sendDelete}
      isLoading={setLoading}
      itemName={'periphery type'}
    />
  )
}

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

const List = (p: Omit<GenericListProps<ControllerItemProps>, 'count'>) => {
  const count = useCtlSelector(s => (getKnownEntities(s) || []).length)
  return <GenericList {...p} count={count} />
}
export const InnerList = () => {
  const send = useSendCommand()
  const sendDelete = (id: IdType) => send('delete-controller', id)
  return (
    <div className={styles.container}>
      <h1>List of controllers</h1>
      <AddButton className={styles.add} text={'Add new controller'} />

      <WaitLoading isLoadingSelector={getIsLoading}>
        <List
          containerClassName={styles.list}
          sendDelete={sendDelete}
          Item={Item}
          listConfigCss={{
            itemMaxHeight: '400px',
            columnMin: 'min-content',
            columnMax: 'auto'
          }}
        />
      </WaitLoading>
    </div>
  )
}
