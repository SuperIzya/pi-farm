import React, { Dispatch } from 'react'
import * as styles from './periphery-form.scss'
import { getKnownTypes as getKnownPeriphery } from '../periphery-types/selectors'
import { connect } from 'react-redux'
import { getNewType } from './selectors'
import { removeNewTypePeriphery, addNewTypePeriphery } from './actions'
import { RootState } from './types'
import InputLabel from '@mui/material/InputLabel'
import Button from '@mui/material/Button'
import { createSelector } from 'reselect'
import DeleteIcon from '@mui/icons-material/Delete'
import SaveAltIcon from '@mui/icons-material/SaveAlt'
import AddIcon from '@mui/icons-material/Add'
import MenuItem from '@mui/material/MenuItem'
import { createList, type ItemProps } from '../../../utils/list-mixin'
import Select from '@mui/material/Select'
import TextField from '@mui/material/TextField'
import { PayloadAction } from '@reduxjs/toolkit'

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

const mapPeriphery = (state: RootState, { itemKey }: ItemProps) => ({
  name: getKnownPeriphery(state)[itemKey].name,
  id: getKnownPeriphery(state)[itemKey].id
})

const PeripheryListItem = connect(mapPeriphery)(({ name, id }: NewItemProps) => (
  <MenuItem value={id}>{name}</MenuItem>
))

const List = createList(getKnownPeriphery)(({ itemKey }: ItemProps) => (
  <PeripheryListItem itemKey={itemKey} />
))

type PeripheryListProps = {
  selected?: number
  onSelect: (key: number) => void
}

const PeripherySelect = ({ selected, onSelect }: PeripheryListProps) => (
  <Select value={selected || ''} onChange={(e) => onSelect(Number(e.target.value))}>
    <List />
  </Select>
)

const NewPeriphery = ({ save }: SaveProps) => {
  const [key, setKey] = React.useState<string>('')
  const [id, setId] = React.useState<number | undefined>(undefined)
  const onSave = () => {
    if (key && id !== undefined) {
      save(key, id)
    }
  }
  return (
    <div className={styles.line}>
      <TextField
        id="outlined-basic"
        label="Identifier"
        variant="outlined"
        value={key}
        onChange={(e) => setKey(e.target.value)}
      />
      <PeripherySelect selected={id} onSelect={setId} />
      <Button variant={'contained'} onClick={onSave}>
        <SaveAltIcon />
      </Button>
    </div>
  )
}

const idSelected = (_: RootState, { id }: { id: number }) => id
const nameSelector = () =>
  createSelector(
    [getKnownPeriphery, idSelected],
    (periphery, id) => periphery.find((p) => p.id === id)?.name || ''
  )
const imageSelector = () =>
  createSelector(
    [getKnownPeriphery, idSelected],
    (periphery, id) => periphery.find((p) => p.id === id)?.image || ''
  )

const Name = connect(() => {
  const selector = nameSelector()
  return (state: RootState, prop: IdProp) => ({
    name: selector(state, prop)
  })
})(({ name }: { name: string }) => <span className={styles.name}>{name}</span>)

const Image = connect(() => {
  const selector = imageSelector()
  return (state: RootState, prop: IdProp) => ({
    image: selector(state, prop)
  })
})(({ image }: { image: string }) => (
  <img src={image} alt="Periphery Type" className={styles.picture} />
))

const newPeriphery = createSelector([getNewType], (tpe) => tpe?.peripheries || {})
const peripheriesKeys = createSelector([newPeriphery], (periphery) =>
  Object.keys(periphery)
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

const ConnectedPeripheryItem = connect(() => {
  const selector = peripheryItemSelector()
  return (state: RootState, props: ItemProps<RemoveProps>) => ({
    ...selector(state, props)
  })
})(({ id, itemKey, remove }: IdProp & RemoveProps & PeripheryKey) => (
  <div className={styles.item}>
    <Image id={id} />
    <Name id={id} />
    <Button variant={'contained'} onClick={() => remove(itemKey)}>
      <DeleteIcon />
    </Button>
  </div>
))

const PeripheryItem = (props: ItemProps<RemoveProps>) => (
  <ConnectedPeripheryItem {...props} />
)

const listCreator = createList(peripheriesKeys)
const PeripheriesList = listCreator<RemoveProps>(PeripheryItem)

const dispatchPeripheryForm = (dispatch: Dispatch<PayloadAction<unknown>>) => ({
  save: (key: string, id: number) => dispatch(addNewTypePeriphery({ [key]: id })),
  remove: (key: string) => dispatch(removeNewTypePeriphery(key))
})

export const PeripheryForm = connect(
  null,
  dispatchPeripheryForm
)(({ save, remove }: SaveProps & RemoveProps) => {
  const [showNew, setShowNew] = React.useState<boolean>(false)

  return (
    <div className={styles.form}>
      <InputLabel id="periphery-label">Periphery</InputLabel>
      <PeripheriesList remove={remove} />
      {showNew ? (
        <NewPeriphery save={save} />
      ) : (
        <Button onClick={() => setShowNew(true)}>
          <AddIcon />
        </Button>
      )}
    </div>
  )
})
