import React from 'react'
import { useSendCommand } from '../../client'
import type { Configuration, IdType, ClassName, ConfigurationId, WithSelector } from '../../types'
import * as styles from './list.scss'
import { DeleteButton, EditButton } from '../../utils/form-mixin'
import { getIsLoading, getKnownEntities } from './selectors'
import {
  GenericList,
  WithKey,
  type GenericListProps,
  type ListItem
} from '../../utils/list-mixin'
import { createSelector } from 'reselect'
import { Text } from '../../utils/text'
import { setLoading } from './actions'
import { RootState } from './types'
import { useSelector } from 'react-redux'
import { ExportData, InventoryPage } from '../page'

type ConfSelProps = WithSelector<Configuration, RootState>
const useCSelector = useSelector.withTypes<RootState>()

const TextComponent = ({ text, className }: { text: string } & ClassName) => (
  <Text className={className} text={text} />
)

const Name = ({ className, selector }: ClassName & ConfSelProps) => {
  const name = useCSelector(createSelector(selector, p => p?.name || ''))
  return <TextComponent text={name} className={className} />
}

const Description = ({ className, selector }: ClassName & ConfSelProps) => {
  const description = useCSelector(createSelector(selector, p => p?.description || ''))
  return <TextComponent text={description} className={className} />
}

const SvgPreview = ({ className, selector }: ClassName & ConfSelProps) => {
  const svg = useCSelector(createSelector(selector, p => p?.graphData?.svg))
  return svg ? <img className={className} src={svg} alt='Graph preview' /> : null
}

type ConfigurationItemProps = {
  sendDelete: (id: IdType) => void
}

const EditBtn = ({ selector }: WithSelector<ConfigurationId, RootState>) => {
  const id = useCSelector(selector)
  return <EditButton className={styles.editButton} id={id ?? -1} />
}

const DeleteBtn = ({
  selector,
  sendDelete
}: WithSelector<ConfigurationId, RootState> & ConfigurationItemProps) => {
  const id = useCSelector(selector)
  return (
    <DeleteButton
      id={id ?? -1}
      className={styles.deleteButton}
      onDelete={sendDelete}
      isLoading={setLoading}
      itemName={'configuration'}
    />
  )
}
const Item: ListItem<Configuration, RootState, ConfigurationItemProps> = ({
  selector,
  sendDelete
}) => {
  const idSelector = createSelector(selector, p => p?.id)
  const id = useCSelector(idSelector)
  return (
    <div className={styles.item}>
      <Name selector={selector} className={styles.name} />
      <Description selector={selector} className={styles.description} />
      <SvgPreview selector={selector} className={styles.preview} />
      <ExportData className={styles.exportButton} id={id ?? -1} command={'configuration'} />
      <EditBtn selector={idSelector} />
      <DeleteBtn selector={idSelector} sendDelete={sendDelete} />
    </div>
  )
}

const List = (
  p: Omit<
    GenericListProps<Configuration, RootState, ConfigurationItemProps>,
    'count' | 'selectorFactory'
  >
) => {
  const count = useCSelector(getKnownEntities).length
  const selector = (idx: number) => createSelector(getKnownEntities, entities => entities[idx])
  return (
    <GenericList<Configuration, RootState, ConfigurationItemProps>
      {...p}
      count={count}
      selectorFactory={selector}
    />
  )
}

export const InnerList = () => {
  const send = useSendCommand()
  const sendDelete = (id: IdType) => send('delete-configuration', id)
  return (
    <InventoryPage<RootState>
      styles={styles}
      getDataCommand={'get-configurations'}
      getIsLoading={getIsLoading}
      title={'List of configurations'}
      addEntityText={'Add new configuration'}
    >
      <List containerClassName={styles.list} sendDelete={sendDelete} Item={Item} />
    </InventoryPage>
  )
}
