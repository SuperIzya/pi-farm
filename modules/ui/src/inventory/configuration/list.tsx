import React from 'react'
import { useSendCommand } from '../../client'
import type { Configuration, IdType, SelectorProps, ClassName } from '../../types'
import * as styles from './list.scss'
import { DeleteButton, EditButton } from '../form-mixin'
import { getIsLoading, getKnownEntities } from './selectors'
import { GenericList, type GenericListProps, type ListItem } from '../../utils/list-mixin'
import { createSelector } from 'reselect'
import { Text } from '../../utils/text'
import { setLoading } from './actions'
import { RootState } from './types'
import { useSelector } from 'react-redux'
import { ExportData, InventoryPage } from '../page'

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
  const { id } = useCSelector(selector)
  return (
    <div className={styles.item}>
      <Name selector={selector} className={styles.name} />
      <Description selector={selector} className={styles.description} />
      <SvgPreview selector={selector} className={styles.preview} />
      <ExportData className={styles.exportButton} id={id} command={'configuration'} />
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
    <InventoryPage<RootState>
      styles={styles}
      getIsLoading={getIsLoading}
      title={'List of configurations'}
      addEntityText={'Add new configuration'}
    >
      <List containerClassName={styles.list} sendDelete={sendDelete} Item={Item} />
    </InventoryPage>
  )
}
