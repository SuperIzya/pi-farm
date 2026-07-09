import React from 'react'
import { useSendCommand } from '../../client'
import type { Configuration, IdType } from '../../types'
import * as styles from './list.scss'
import { AddButton, ClassName, DeleteButton, EditButton } from '../form-mixin'
import { WaitLoading } from '../../utils/wait-loading'
import { getIsLoading, getKnownEntities } from './selectors'
import { GenericList, GenericListProps, getListKey, ListItem } from '../../utils/list-mixin'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'
import { Text } from '../../utils/text'
import { setLoading } from './actions'

const configurationSelector = <T,>(f: (c: Configuration) => T) =>
  createSelector([getKnownEntities, getListKey], (configurations, itemKey) =>
    f(configurations[itemKey])
  )

const configurationIdSelector = () => configurationSelector(({ id }) => ({ id }))

const TextComponent = ({ text, className }: { text: string } & ClassName) => (
  <Text className={className} text={text} />
)

const Name = connect(() => configurationSelector(({ name: text }) => ({ text })))(TextComponent)

const Description = connect(() => configurationSelector(({ description: text }) => ({ text })))(
  TextComponent
)

const SvgPreview = connect(() =>
  configurationSelector(({ graphData }) => ({ svg: graphData.svg }))
)(({ svg, className }: { svg?: string } & ClassName) =>
  svg ? <img className={className} src={svg} alt='Graph preview' /> : null
)

type ConfigurationItemProps = {
  sendDelete: (id: IdType) => void
}

const connectId = connect(configurationIdSelector)

const EditBtn = connectId(({ id }: { id: IdType }) => (
  <EditButton className={styles.editButton} id={id} />
))

const DeleteBtn = connectId(({ id, sendDelete }: { id: IdType } & ConfigurationItemProps) => (
  <DeleteButton
    id={id}
    className={styles.deleteButton}
    onDelete={sendDelete}
    isLoading={setLoading}
    itemName={'configuration'}
  />
))

const Item: ListItem<ConfigurationItemProps> = ({ itemKey, sendDelete }) => (
  <div className={styles.item}>
    <Name itemKey={itemKey} className={styles.name} />
    <Description itemKey={itemKey} className={styles.description} />
    <SvgPreview itemKey={itemKey} className={styles.preview} />
    <EditBtn itemKey={itemKey} />
    <DeleteBtn sendDelete={sendDelete} itemKey={itemKey} />
  </div>
)

const mapCount = createSelector([getKnownEntities], ({ length }) => ({ count: length }))
const List = connect(mapCount)((p: GenericListProps<ConfigurationItemProps>) => (
  <GenericList {...p} />
))

export const InnerList = () => {
  const send = useSendCommand()
  const sendDelete = (id: IdType) => send('delete-configuration', id)
  return (
    <div className={styles.container}>
      <h1>List of configurations</h1>
      <AddButton className={styles.add} text={'Add new configuration'} />

      <WaitLoading isLoadingSelector={getIsLoading}>
        <List containerClassName={styles.list} sendDelete={sendDelete} Item={Item} />
      </WaitLoading>
    </div>
  )
}
