import React, { Dispatch } from 'react'
import * as styles from './periphery-form.scss'
import { getKnownTypes as getKnownPeriphery } from '../periphery-types/selectors'
import { connect, useSelector } from 'react-redux'
import { getNewType } from './selectors'
import { removeNewTypePeriphery, addNewTypePeriphery } from './actions'
import { RootState } from './types'
import InputLabel from '@mui/material/InputLabel'
import Button from '@mui/material/Button'
import { createSelector } from 'reselect'
import DeleteIcon from '@mui/icons-material/Delete'
import AddIcon from '@mui/icons-material/Add'
import MenuItem from '@mui/material/MenuItem'
import { createList, type ItemProps } from '../../../utils/list-mixin'
import Select from '@mui/material/Select'
import TextField from '@mui/material/TextField'
import { PayloadAction } from '@reduxjs/toolkit'
import { ClassName } from '../form-mixin'
import classNames from 'classnames'

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

type PeripheryTypeItemProps = {
  name: string
  id: number
}
const knownPeripheriesSelector = createSelector(getKnownPeriphery, (periphery) =>
  periphery.map((p): PeripheryTypeItemProps => ({ name: p.name, id: p.id }))
)

const NewPeriphery = ({ save }: SaveProps) => {
  const knownPeriphery = useSelector(knownPeripheriesSelector)
  const [key, setKey] = React.useState<string>('')
  const [id, setId] = React.useState<number | undefined>(undefined)
  const onSave = () => {
    if (key && id !== undefined) {
      save(key, id)
    }
  }
  return (
    <div className={styles.new}>
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
      <Button variant={'contained'} onClick={onSave} className={styles.addButton}>
        <AddIcon />
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
  <img src={image} alt="Periphery Type" className={styles.image} />
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
    <Button
      variant={'contained'}
      onClick={() => remove(itemKey)}
      className={styles.deleteButton}
    >
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
)(({ save, remove, className }: SaveProps & RemoveProps & ClassName) => {
  const [showNew, setShowNew] = React.useState<boolean>(false)

  const onSave = (key: string, id: number) => {
    save(key, id)
    setShowNew(false)
  }

  return (
    <div className={classNames(styles.form, className)}>
      <InputLabel id="periphery-label">Periphery</InputLabel>
      <PeripheriesList
        remove={remove}
        containerClassName={styles.list}
        listConfigCss={{
          columns: 2,
          gridGap: '5px',
          maxWidth: '50%',
          columnMin: '75px',
          columnMax: '150px'
        }}
      />
      {showNew ? (
        <NewPeriphery save={onSave} />
      ) : (
        <Button onClick={() => setShowNew(true)} className={styles.addButton}>
          <AddIcon />
        </Button>
      )}
    </div>
  )
})
