import React from 'react'
import { useSendCommand } from '../../client'
import type { Configuration, IdType, SelectorProps } from '../../types'
import * as styles from './list.scss'
import { AddButton, ClassName, DeleteButton, EditButton } from '../form-mixin'
import { WaitLoading } from '../../utils/wait-loading'
import { getIsLoading, getKnownEntities } from './selectors'
import { GenericList, GenericListProps, ListItem } from '../../utils/list-mixin'
import { createSelector } from 'reselect'
import { Text } from '../../utils/text'
import { setLoading } from './actions'
import { RootState } from './types'
import { useSelector } from 'react-redux'

type ConfSelProps = SelectorProps<Configuration, RootState>
const useCSelector = useSelector.withTypes<RootState>()

const TextComponent = ({ text, className }: { text: string } & ClassName) => (
  <Text className={className} text={text} />
)

const Name = ({ className, selector }: ClassName & ConfSelProps) => {
  const { name } = useCSelector(selector)
  return <TextComponent text={name} className={className} />
}

const Description = ({ className, selector }: ClassName & ConfSelProps) => {
  const { description } = useCSelector(selector)
  return <TextComponent text={description} className={className} />
}

const SvgPreview = ({ className, selector }: ClassName & ConfSelProps) => {
  const { graphData } = useCSelector(selector)
  const svg = graphData.svg
  return svg ? <img className={className} src={svg} alt='Graph preview' /> : null
}

type ConfigurationItemProps = {
  sendDelete: (id: IdType) => void
}

const EditBtn = ({ selector }: ConfSelProps) => {
  const { id } = useCSelector(selector)
  return <EditButton className={styles.editButton} id={id} />
}

const DeleteBtn = ({ selector, sendDelete }: ConfSelProps & ConfigurationItemProps) => {
  const { id } = useCSelector(selector)
  return (
    <DeleteButton
      id={id}
      className={styles.deleteButton}
      onDelete={sendDelete}
      isLoading={setLoading}
      itemName={'configuration'}
    />
  )
}
const Item: ListItem<ConfigurationItemProps> = ({ itemKey, sendDelete }) => {
  const selector = createSelector(getKnownEntities, entities => entities[itemKey])
  return (
    <div className={styles.item}>
      <Name selector={selector} className={styles.name} />
      <Description selector={selector} className={styles.description} />
      <SvgPreview selector={selector} className={styles.preview} />
      <EditBtn selector={selector} />
      <DeleteBtn selector={selector} sendDelete={sendDelete} />
    </div>
  )
}

const List = (p: Omit<GenericListProps<ConfigurationItemProps>, 'count'>) => {
  const count = useCSelector(getKnownEntities).length
  return <GenericList {...p} count={count} />
}

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
