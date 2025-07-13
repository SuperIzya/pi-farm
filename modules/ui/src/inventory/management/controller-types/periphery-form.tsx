import React from 'react'
import styles from './periphery-form.scss'
import { getKnownTypes as getKnownPeriphery } from '../periphery-types/selectors'
import { connect } from 'react-redux'
import { createFormRoutines, FormArgs, mapSave } from '../form'
import { getNewType } from './selectors'
import { setNewTypePeriphery } from './actions'
import { RootState } from './types'
import InputLabel from '@mui/material/InputLabel'
import Button from '@mui/material/Button'
import { createSelector } from 'reselect'
import classNames from 'classnames'

type IdProp = {
  id: number
  onClick: (id: number) => void
}

type ItemProp = IdProp & {
  isSelected: boolean
}

type ListProps = {
  ids: number[]
  onClick: (id: number) => void
  selected: number[]
}

const { mapField } = createFormRoutines(getNewType)

const idSelected = (_: RootState, { id }: { id: number }) => id
const nameSelector = () =>
  createSelector(
    [getKnownPeriphery, idSelected],
    (periphery, id) => periphery.find((p) => p.id === id)?.name || ''
  )
const pictureSelector = () =>
  createSelector(
    [getKnownPeriphery, idSelected],
    (periphery, id) => periphery.find((p) => p.id === id)?.picture || ''
  )

const Name = connect(() => {
  const selector = nameSelector()
  return (state: RootState, prop: IdProp) => ({
    name: selector(state, prop)
  })
})(({ name, id, onClick }: IdProp & { name: string }) => (
  <span className={styles.name} onClick={() => onClick(id)}>
    {name}
  </span>
))

const Picture = connect(() => {
  const selector = pictureSelector()
  return (state: RootState, prop: IdProp) => ({
    picture: selector(state, prop)
  })
})(({ picture, id, onClick }: IdProp & { picture: string }) => (
  <img
    src={picture}
    alt="Periphery Type"
    className={styles.picture}
    onClick={() => onClick(id)}
  />
))

const PeripheryItem = ({ id, onClick, isSelected }: ItemProp) => (
  <div
    className={classNames(styles.item, isSelected && styles.selected)}
    onClick={() => onClick(id)}
  >
    <Picture id={id} onClick={onClick} />
    <Name id={id} onClick={onClick} />
  </div>
)

const PeripheryList = ({ ids, onClick, selected }: ListProps) => (
  <div className={styles.list}>
    {ids.map((id) => (
      <PeripheryItem
        key={id}
        id={id}
        onClick={onClick}
        isSelected={selected.includes(id)}
      />
    ))}
  </div>
)
const knownIdsSelector = createSelector(getKnownPeriphery, (periphery) =>
  periphery.map(({ id }) => id)
)
const KnownPeripheryList = connect((state: RootState) => ({
  ids: knownIdsSelector(state)
}))(PeripheryList)

const mapPeripheryForm = mapField(({ periphery }) => periphery || [])
const savePeripheryForm = mapSave(setNewTypePeriphery)

export const PeripheryForm = connect(
  mapPeripheryForm,
  savePeripheryForm
)(({ original, save }: FormArgs<number[]>) => {
  const [toAdd, setToAdd] = React.useState<number[]>([])
  const [toRemove, setToRemove] = React.useState<number[]>([])

  const onAllClick = (id: number) => {
    if (toAdd.includes(id)) {
      setToAdd(toAdd.filter((i) => i !== id))
    } else setToAdd([...toAdd, id])
  }

  const onSelectedClick = (id: number) => {
    if (toRemove.includes(id)) {
      setToRemove(toRemove.filter((i) => i !== id))
    } else setToRemove([...toRemove, id])
  }

  const removeSelected = () => {
    save(original.filter((id) => !toRemove.includes(id)))
    setToRemove([])
  }
  const addSelected = () => {
    save([...original, ...toAdd])
    setToAdd([])
  }

  return (
    <div className={styles.form}>
      <InputLabel id="periphery-label">Periphery</InputLabel>
      <PeripheryList ids={original} onClick={onSelectedClick} selected={toRemove} />
      <div>
        <Button variant={'text'} onClick={removeSelected}>
          -&gt;
        </Button>
        <Button variant={'text'} onClick={addSelected}>
          &lt;-
        </Button>
      </div>
      <KnownPeripheryList onClick={onAllClick} selected={toAdd} />
    </div>
  )
})
