import React, { Dispatch, useEffect, useState } from 'react'
import { bindActionCreators, PayloadAction, PayloadActionCreator } from '@reduxjs/toolkit'
import { connect, useDispatch, useSelector } from 'react-redux'
import Button from '@mui/material/Button'
import { NewType, WithId } from './types'
import type { ItemProps } from '../../utils/list-mixin'
import { useNavigate, useParams } from 'react-router'
import IconButton from '@mui/material/IconButton'
import EditIcon from '@mui/icons-material/Edit'
import TextField from '@mui/material/TextField'
import DeleteForeverIcon from '@mui/icons-material/DeleteForever'
import { Dialog, DialogActions, DialogTitle } from '@mui/material'
import * as styles from './form-mixin.scss'
import ThumbUpIcon from '@mui/icons-material/ThumbUp'
import ThumbDownIcon from '@mui/icons-material/ThumbDown'
import { createSelector } from 'reselect'

export type OriginalArgs<T = string> = { original: T | undefined }
export type SaveArgs<T = string> = { save: (value: T | undefined) => void }
export type ClassName = { className?: string }
export type FormArgs<T = string> = OriginalArgs<T> &
  SaveArgs<T> &
  ClassName & { multiline?: boolean }

type KnownObjectsExtractor<S, T> = (state: S) => T[]
type ObjExtractor<S, T> = (s: S) => Partial<T> | undefined
type FieldExtractor<T, Q> = (p: Partial<T>) => Q

const mapField =
  <S, T, Out>(objExtractor: ObjExtractor<S, T>, fieldExtractor: FieldExtractor<T, Out>) =>
  (state: S) => ({
    original: fieldExtractor(objExtractor(state) || {})
  })

export const mapSave =
  <T = string,>(creator: PayloadActionCreator<T>) =>
  (dispatch: Dispatch<PayloadAction<T>>) => ({
    save: (value: T) => dispatch(creator(value))
  })

export const formTextField =
  <S, T>(objExtractor: ObjExtractor<S, T>) =>
  (
    creator: PayloadActionCreator<string | undefined>,
    fieldExtractor: FieldExtractor<T, string | undefined>,
    label: string
  ) =>
    connect(
      mapField(objExtractor, fieldExtractor),
      mapSave(creator)
    )(({ original, save, className, multiline }: FormArgs) => {
      const [name, setName] = useState(original)

      useEffect(() => setName(original), [original])
      return (
        <TextField
          required
          id="outlined-required"
          label={label}
          variant="outlined"
          className={className}
          multiline={multiline ?? false}
          onChange={(e) => setName(e.target.value)}
          onBlur={() => save(name)}
          value={name || ''}
        />
      )
    })

export const formSaveButton =
  <S, T>(
    objExtractor: ObjExtractor<S, NewType<T> | undefined>,
    saveNewType: PayloadActionCreator,
    setLoading: PayloadActionCreator<boolean>
  ) =>
  ({ className }: ClassName) => {
    const canBeSavedSelector = createSelector(
      [(s: S) => objExtractor(s)],
      (t: Partial<NewType<T>> | undefined) => t?.canBeSaved || false
    )
    const canBeSaved = useSelector(canBeSavedSelector)
    const navigate = useNavigate()
    const dispatch = useDispatch()
    const onClick = () => {
      if (!canBeSaved) return
      dispatch(setLoading(true))
      dispatch(saveNewType())
      navigate('..')
    }
    return (
      <Button
        variant="contained"
        color="primary"
        onClick={onClick}
        className={className}
        disabled={!canBeSaved}
      >
        Save
      </Button>
    )
  }

export const cancelButton =
  (cancelNewType: PayloadActionCreator) =>
  ({ className }: ClassName) => {
    const navigate = useNavigate()
    const dispatch = useDispatch()
    const cancel = () => {
      dispatch(cancelNewType())
      navigate('..')
    }

    return (
      <Button variant="outlined" color="secondary" onClick={cancel} className={className}>
        Cancel
      </Button>
    )
  }

export const formMapField =
  <S, T>(objectExtractor: ObjExtractor<S, T>) =>
  <Out,>(fieldExtractor: FieldExtractor<T, Out>) =>
    mapField(objectExtractor, fieldExtractor)

type EditOrNewProps = {
  children: React.ReactNode
  editType: (id: number) => void
  newType: () => void
  label: string
}

export const formEditOrNew = (
  newType: PayloadActionCreator,
  editType: PayloadActionCreator<number>
) =>
  connect(null, (dispatch) => bindActionCreators({ editType, newType }, dispatch))(
    ({ children, editType, newType, label }: EditOrNewProps) => {
      const params = useParams<{ id?: string }>()
      let isEdit = false
      if (params.id !== null && !isNaN(Number(params.id))) {
        editType(Number(params.id))
        isEdit = true
      } else {
        newType()
      }

      return (
        <>
          <h3>
            {isEdit ? 'Edit' : 'New'} {label}
          </h3>
          {children}
        </>
      )
    }
  )

type FormButtonProps<S, T extends { id: number }> = {
  className: string
  objectsExtractor: KnownObjectsExtractor<S, T>
  onClick: (id: number) => void
  Icon: () => React.ReactNode
} & ItemProps

type AddButtonProps = {
  className: string
  text: string
}
export const AddButton = ({ className, text }: AddButtonProps) => {
  const navigate = useNavigate()
  return (
    <div className={className}>
      <Button onClick={() => navigate('new')}>{text}</Button>
    </div>
  )
}

const GenericButton = <S, T extends { id: number }>({
  className,
  objectsExtractor,
  itemKey,
  onClick,
  Icon
}: FormButtonProps<S, T>) => {
  const mapId = () => (state: S) => ({
    id: objectsExtractor(state)[itemKey].id
  })

  const C = connect(mapId)(({ id }: { id: number }) => (
    <IconButton className={className} onClick={() => onClick(id)}>
      <Icon />
    </IconButton>
  ))

  return <C />
}

type EditButtonProps<S, T extends WithId> = {
  className: string
  itemKey: number
  objectsExtractor: KnownObjectsExtractor<S, T>
}

export const EditButton = <S, T extends { id: number }>({
  className,
  objectsExtractor,
  itemKey
}: EditButtonProps<S, T>) => {
  const navigate = useNavigate()
  const onClick = (id: number) => navigate(`edit/${id}`)
  return (
    <GenericButton
      className={className}
      objectsExtractor={objectsExtractor}
      itemKey={itemKey}
      onClick={onClick}
      Icon={() => <EditIcon />}
    />
  )
}

type DeleteButtonProps<S, T extends WithId> = {
  className: string
  itemKey: number
  itemName: string
  sendDelete: (id: number) => void
  isLoading: PayloadActionCreator<boolean>
  objectsExtractor: KnownObjectsExtractor<S, T>
}

export const DeleteButton = <S, T extends { id: number }>({
  className,
  itemKey,
  isLoading,
  itemName,
  sendDelete,
  objectsExtractor
}: DeleteButtonProps<S, T>) => {
  const dispatch = useDispatch()
  const [currentId, setCurrentId] = useState<number | null>(null)
  const [open, setOpen] = React.useState(false)
  const [agree, setAgree] = React.useState(false)
  const onClick = (id: number) => {
    setCurrentId(id)
    setOpen(true)
  }
  const deleteId = (id: number) => {
    dispatch(isLoading(true))
    sendDelete(id)
  }
  useEffect(() => {
    if (currentId !== null && agree) deleteId(currentId)
    setOpen(false)
  }, [agree, currentId])

  const onAgree = () => {
    setAgree(true)
    setOpen(false)
  }
  const onDisagree = () => {
    setAgree(false)
    setOpen(false)
  }
  return (
    <>
      <GenericButton
        className={className}
        objectsExtractor={objectsExtractor}
        onClick={onClick}
        Icon={() => <DeleteForeverIcon />}
        itemKey={itemKey}
      />
      <Dialog open={open} onClose={onDisagree}>
        <DialogTitle>
          Are you sure you want to delete {itemName} #{currentId}
        </DialogTitle>
        <DialogActions>
          <IconButton onClick={onAgree} className={styles.agree}>
            <ThumbUpIcon color={'success'} />
          </IconButton>
          <IconButton onClick={onDisagree} className={styles.disagree}>
            <ThumbDownIcon color={'error'} />
          </IconButton>
        </DialogActions>
      </Dialog>
    </>
  )
}
