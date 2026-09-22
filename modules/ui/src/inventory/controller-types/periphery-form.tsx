import React from 'react'
import * as styles from './periphery-form.scss'
import { getImage, getKnownEntities as getKnownPeriphery } from '../periphery-types/selectors'
import { useDispatch, useSelector } from 'react-redux'
import { getNewEntity, sortPeripheriesKeys } from './selectors'
import { addNewEntityPeriphery, removeNewEntityPeriphery } from './actions'
import { RootState } from './types'
import InputLabel from '@mui/material/InputLabel'
import { createSelector } from 'reselect'
import DeleteIcon from '@mui/icons-material/Delete'
import AddIcon from '@mui/icons-material/Add'
import MenuItem from '@mui/material/MenuItem'
import { GenericList, WithKey, type GenericListProps, type ItemProps } from '../../utils/list-mixin'
import Select from '@mui/material/Select'
import TextField from '@mui/material/TextField'
import classNames from 'classnames'
import IconButton from '@mui/material/IconButton'
import type { ClassName, IdType, PeripheryType, WithSelector } from '../../types'
import { Text } from '../../utils/text'

type WithRemove = {
  remove: (key: string) => void
}

type SaveProps = {
  save: (key: string, id: number) => void
}

type NewItemProps = {
  name: string
  id: number
}

type PeripheryListProps = {
  selected?: number
  onSelect: (key: number) => void
  knownPeripheries?: NewItemProps[]
} & ClassName

const usePSelector = useSelector.withTypes<RootState>()
type LeafProps = WithSelector<PeripheryType, RootState>

const PeripherySelect = ({
  selected,
  onSelect,
  knownPeripheries,
  className
}: PeripheryListProps) => (
  <Select
    className={className}
    value={selected || ''}
    onChange={e => onSelect(Number(e.target.value))}
  >
    {knownPeripheries && knownPeripheries.map(p => <MenuItem value={p.id}>{p.name}</MenuItem>)}
  </Select>
)

const knownPeripheriesSelector = createSelector([getKnownPeriphery], periphery =>
  periphery.map(({ name, id }) => ({ name, id }))
)

const NewPeriphery = ({ save }: SaveProps) => {
  const knownPeriphery = useSelector(knownPeripheriesSelector)
  const [key, setKey] = React.useState<string>('')
  const [id, setId] = React.useState<IdType | undefined>(undefined)
  const onSave = () => {
    if (key && id !== undefined) {
      save(key, id)
      setKey('')
      setId(undefined)
    }
  }
  return (
    <div className={styles.peripheryForm}>
      <div className={styles.image}>
        {id !== undefined && (
          <Image
            selector={(state: RootState) => getKnownPeriphery(state)!.find(p => p.id === id)!}
          />
        )}
      </div>
      <TextField
        id='outlined-basic'
        label='Identifier'
        variant='outlined'
        value={key}
        className={styles.identifier}
        onChange={e => setKey(e.target.value)}
      />
      <PeripherySelect
        selected={id}
        className={styles.periphery}
        knownPeripheries={knownPeriphery}
        onSelect={setId}
      />
      <IconButton onClick={onSave} className={styles.addButton}>
        <AddIcon />
      </IconButton>
    </div>
  )
}

const selector = (id: IdType) =>
  createSelector(getKnownPeriphery, periphery => periphery.find(p => p.id === id)!)

const Name = ({ selector }: LeafProps) => {
  const name = usePSelector(createSelector(selector, item => item?.name || ''))
  return <Text className={styles.name} text={name} />
}

const Image = ({ selector }: LeafProps) => {
  const { name, image } = usePSelector(
    createSelector(selector, item => ({
      name: item?.name || '',
      image: getImage(item)
    }))
  )
  return (
    <div className={styles.image}>
      <img src={image} alt={name} />
    </div>
  )
}

const Key = ({ name }: { name: string }) => (
  <div className={styles.key}>
    <span>{name}</span>
  </div>
)

const newPeriphery = createSelector(getNewEntity, tpe => tpe?.peripheries || {})
const peripheriesKeys = createSelector(newPeriphery, periphery =>
  sortPeripheriesKeys(Object.keys(periphery))
)

type PeripheryItem = {
  id: IdType
  key: string
}

const peripheryItemSelector = (itemKey: number) =>
  createSelector(newPeriphery, peripheriesKeys, (periphery, keys): PeripheryItem => ({
    id: periphery[keys[itemKey]],
    key: keys[itemKey]
  }))

const PeripheryItem = ({
  itemKey,
  remove
}: ItemProps<PeripheryItem, RootState, WithRemove & WithKey>) => {
  const { id, key } = usePSelector(peripheryItemSelector(itemKey))
  const itemSelector = selector(id)
  return (
    <div className={styles.item}>
      <Key name={key} />
      <Image selector={itemSelector} />
      <Name selector={itemSelector} />
      <IconButton className={styles.deleteButton} onClick={() => remove(key)}>
        <DeleteIcon />
      </IconButton>
    </div>
  )
}

const PeripheriesList = (
  p: Omit<GenericListProps<PeripheryItem, RootState, WithRemove>, 'count'>
) => {
  const count = usePSelector(peripheriesKeys).length
  return <GenericList<PeripheryItem, RootState, WithRemove> {...p} count={count} />
}

export const PeripheryForm = ({ className }: ClassName) => {
  const dispatch = useDispatch()
  const save = (key: string, id: number) => dispatch(addNewEntityPeriphery({ [key]: id }))
  const remove = (key: string) => dispatch(removeNewEntityPeriphery(key))
  return (
    <div className={classNames(styles.form, className)}>
      <InputLabel id='periphery-label'>Periphery</InputLabel>
      <NewPeriphery save={save} />
      <PeripheriesList
        remove={remove}
        containerClassName={styles.peripheryList}
        listConfigCss={{
          columns: 6,
          columnMin: '100px',
          columnMax: '200px',
          maxWidth: '100%',
          itemMaxHeight: '150px'
        }}
        Item={PeripheryItem}
        selectorFactory={peripheryItemSelector}
      />
    </div>
  )
}
