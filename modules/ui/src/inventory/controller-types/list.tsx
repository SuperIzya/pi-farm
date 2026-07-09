import React from 'react'
import * as styles from './list.scss'
import { GenericList, GenericListProps, ItemProps, ListItem } from '../../utils/list-mixin'
import { getKnownEntities, getIsLoading, useCTSelector } from './selectors'
import { EditButton, AddButton, DeleteButton } from '../form-mixin'
import { setLoading } from './actions'
import { useSendCommand } from '../../client'
import { WaitLoading } from '../../utils/wait-loading'
import { Text } from '../../utils/text'
import { IdType } from '../../types'
import { PeripheryList } from './periphery-list'
import { Guard } from '../periphery-types/guard'
import { buildItemSelector } from '../store-mixin'
import { DescriptionIcon, CodeIcon } from '../../utils/icons'

const controllerTypeSelector = buildItemSelector(getKnownEntities)

type ControllerItemProps = {
  sendDelete: (id: number) => void
}
const Name = ({itemKey}: ItemProps) => {
  const { name } = useCTSelector(controllerTypeSelector(itemKey, ({ name }) => ({ name })))
  return <Text className={styles.name} text={name} />
}

const Description = ({itemKey}: ItemProps) => {
  const { description } = useCTSelector(controllerTypeSelector(itemKey, ({ description }) => ({ description })))
  return <Text className={styles.description} text={description} icon={<DescriptionIcon />} />
}

const Schema = ({itemKey}: ItemProps) => {
  const { schema } = useCTSelector(controllerTypeSelector(itemKey, ({ schema }) => ({ schema })))
  return (
    schema && (
      <a href={schema} target='_blank' rel='noreferrer'>
        Schema
      </a>
    )
  )
}

const Code = ({itemKey}: ItemProps) => {
  const { code } = useCTSelector(controllerTypeSelector(itemKey, ({ code }) => ({ code })))
  return <Text className={styles.code} text={code} icon={<CodeIcon />} />
}

const mapId = (itemKey: number) => controllerTypeSelector(itemKey, ({ id }) => ({ id }))

const EditBtn = ({itemKey}: ItemProps) => {
  const { id } = useCTSelector(mapId(itemKey))
  return <EditButton id={id} className={styles.editButton} />
}

const DeleteBtn = ({itemKey, sendDelete}: ItemProps & ControllerItemProps) => {
  const { id } = useCTSelector(mapId(itemKey))
  return (
    <DeleteButton
      id={id}
      className={styles.deleteButton}
      onDelete={sendDelete}
      isLoading={setLoading}
      itemName={'controller type'}
    />
  )
}

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
        columns: 4,
        maxWidth: '100%',
        columnMin: '50px',
        columnMax: '75px'
      }}
    />
    <EditBtn itemKey={itemKey} />
    <DeleteBtn sendDelete={sendDelete} itemKey={itemKey} />
  </div>
)

const List = (props: Omit<GenericListProps<ControllerItemProps>, 'count'>) => {
  const count = useCTSelector(s => (getKnownEntities(s) || []).length)
  return (
    <GenericList {...props} count={count} />
  )
}

export const InnerList = () => {
  const send = useSendCommand()
  const sendDelete = (id: IdType) => send('delete-controller-type', id)
  return (
    <div className={styles.container}>
      <Guard />
      <h1>List of controller types</h1>
      <AddButton className={styles.add} text={'Add new controller type'} />

      <WaitLoading isLoadingSelector={getIsLoading}>
        <List
          containerClassName={styles.list}
          sendDelete={sendDelete}
          Item={Item}
          listConfigCss={{
            itemMaxHeight: '400px', 
            columns: 3, 
            columnMin: 'min-content', 
            columnMax: 'auto' 
          }}
        />
      </WaitLoading>
    </div>
  )
}
