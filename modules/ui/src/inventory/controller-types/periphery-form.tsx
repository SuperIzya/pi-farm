import React, { Dispatch } from 'react'
import * as styles from './periphery-form.scss'
import { getKnownEntities as getKnownPeriphery } from '../periphery-types/selectors'
import { connect, useSelector } from 'react-redux'
import { getNewEntity, sortPeripheriesKeys } from './selectors'
import { addNewEntityPeriphery, removeNewEntityPeriphery } from './actions'
import { RootState } from './types'
import InputLabel from '@mui/material/InputLabel'
import { createSelector } from 'reselect'
import DeleteIcon from '@mui/icons-material/Delete'
import AddIcon from '@mui/icons-material/Add'
import MenuItem from '@mui/material/MenuItem'
import { GenericList, GenericListProps, type ItemProps } from '../../utils/list-mixin'
import Select from '@mui/material/Select'
import TextField from '@mui/material/TextField'
import { PayloadAction } from '@reduxjs/toolkit'
import { ClassName } from '../form-mixin'
import classNames from 'classnames'
import IconButton from '@mui/material/IconButton'
import { IdType } from '../../types'
import { Text } from '../../utils/text'

type IdProp = {
  id: number
}

type RemoveProps = {
  remove: (key: string) => void
}

type SaveProps = {
  save: (key: string, id: number) => void
}

type PeripheryKey = { itemKey: string }

type NewItemProps = {
  name: string
  id: number
}

type PeripheryListProps = {
  selected?: number
  onSelect: (key: number) => void
  knownPeripheries?: NewItemProps[]
} & ClassName

const PeripherySelect = ({
  selected,
  onSelect,
  knownPeripheries,
  className
}: PeripheryListProps) => (
  <Select
    className={className}
    value={selected || ''}
    onChange={(e) => onSelect(Number(e.target.value))}
  >
    {knownPeripheries &&
      knownPeripheries.map((p) => <MenuItem value={p.id}>{p.name}</MenuItem>)}
  </Select>
)

const knownPeripheriesSelector = createSelector([getKnownPeriphery], (periphery) =>
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
      <div className={styles.image}>{id !== undefined && <Image id={id} />}</div>
      <TextField
        id="outlined-basic"
        label="Identifier"
        variant="outlined"
        value={key}
        className={styles.identifier}
        onChange={(e) => setKey(e.target.value)}
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

const idSelected = (_: RootState, { id }: { id: IdType }) => id
const nameSelector = () =>
  createSelector([getKnownPeriphery, idSelected], (periphery, id) => ({
    name: periphery.find((p) => p.id === id)?.name || ''
  }))

const imageSelector = () =>
  createSelector([getKnownPeriphery, idSelected], (periphery, id) => {
    const p = periphery.find((p) => p.id === id)
    return {
      image: p?.image || '',
      name: p?.name || ''
    }
  })

const Name = connect(nameSelector)(({ name }: { name: string }) => (
  <Text className={styles.name} text={name} />
))

const Image = connect(imageSelector)(
  ({ image, name }: { image: string; name: string }) => (
    <img src={image} alt={name} className={styles.image} />
  )
)

const Key = ({ name }: { name: string }) => (
  <div className={styles.key}>
    <span>{name}</span>
  </div>
)

const newPeriphery = createSelector([getNewEntity], (tpe) => tpe?.peripheries || {})
const peripheriesKeys = createSelector([newPeriphery], (periphery) =>
  sortPeripheriesKeys(Object.keys(periphery))
)

const getPropKey = (_: RootState, { itemKey }: ItemProps) => itemKey

const peripheryItemSelector = () =>
  createSelector(
    [newPeriphery, peripheriesKeys, getPropKey],
    (periphery, keys, index) => ({
      id: periphery[keys[index]],
      itemKey: keys[index]
    })
  )

const ConnectedPeripheryItem = connect(peripheryItemSelector)(
  ({ id, itemKey, remove }: IdProp & RemoveProps & PeripheryKey) => (
    <div className={styles.item}>
      <Key name={itemKey} />
      <Image id={id} />
      <Name id={id} />
      <IconButton className={styles.deleteButton} onClick={() => remove(itemKey)}>
        <DeleteIcon />
      </IconButton>
    </div>
  )
)

const PeripheryItem = (props: ItemProps<RemoveProps>) => (
  <ConnectedPeripheryItem {...props} />
)
const getCount = createSelector([peripheriesKeys], ({ length }) => ({ count: length }))
const PeripheriesList = connect(getCount)((p: GenericListProps<RemoveProps>) => (
  <GenericList {...p} />
))

const dispatchPeripheryForm = (dispatch: Dispatch<PayloadAction<unknown>>) => ({
  save: (key: string, id: number) => dispatch(addNewEntityPeriphery({ [key]: id })),
  remove: (key: string) => dispatch(removeNewEntityPeriphery(key))
})

export const PeripheryForm = connect(
  null,
  dispatchPeripheryForm
)(({ save, remove, className }: SaveProps & RemoveProps & ClassName) => {
  return (
    <div className={classNames(styles.form, className)}>
      <InputLabel id="periphery-label">Periphery</InputLabel>
      <NewPeriphery save={save} />
      <PeripheriesList
        remove={remove}
        containerClassName={styles.peripheryList}
        listConfigCss={{
          columns: 6,
          columnMin: 'auto',
          columnMax: 'auto',
          maxWidth: '150px'
        }}
        Item={PeripheryItem}
      />
    </div>
  )
})
