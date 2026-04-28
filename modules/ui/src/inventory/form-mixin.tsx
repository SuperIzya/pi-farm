import React, { Dispatch, useEffect, useState } from 'react'
import { bindActionCreators, PayloadAction, PayloadActionCreator } from '@reduxjs/toolkit'
import { connect, useDispatch, useSelector } from 'react-redux'
import Button from '@mui/material/Button'
import { NewEntity, IdType } from '../types'
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
export type FormArgs<T = string> = OriginalArgs<T>
  & SaveArgs<T>
  & ClassName & {
    multiline?: boolean
    size?: 'small' | 'medium'
    variant?: 'outlined' | 'standard' | 'filled'
  }

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
    )(({ original, save, className, multiline, size, variant }: FormArgs) => {
      const [name, setName] = useState(original)

      useEffect(() => setName(original), [original])
      return (
        <TextField
          required
          id='outlined-required'
          label={label}
          variant={variant || 'outlined'}
          className={className}
          multiline={multiline ?? false}
          onChange={e => setName(e.target.value)}
          onBlur={() => save(name)}
          size={size}
          value={name || ''}
        />
      )
    })

export const formSaveButton =
  <S, T>(
    objExtractor: ObjExtractor<S, NewEntity<T> | undefined>,
    saveNewEntity: PayloadActionCreator,
    setLoading: PayloadActionCreator<boolean>
  ) =>
  ({ className }: ClassName) => {
    const canBeSavedSelector = createSelector(
      [(s: S) => objExtractor(s)],
      (t: Partial<NewEntity<T>> | undefined) => t?.canBeSaved || false
    )
    const canBeSaved = useSelector(canBeSavedSelector)
    const navigate = useNavigate()
    const dispatch = useDispatch()
    const onClick = () => {
      if (!canBeSaved) return
      dispatch(setLoading(true))
      dispatch(saveNewEntity())
      navigate('..')
    }
    return (
      <Button
        variant='contained'
        color='primary'
        onClick={onClick}
        className={className}
        disabled={!canBeSaved}
      >
        Save
      </Button>
    )
  }

export const cancelButton =
  (cancelNewEntity: PayloadActionCreator) =>
  ({ className }: ClassName) => {
    const navigate = useNavigate()
    const dispatch = useDispatch()
    const cancel = () => {
      dispatch(cancelNewEntity())
      navigate('..')
    }

    return (
      <Button variant='outlined' color='secondary' onClick={cancel} className={className}>
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
  editEntity: (id: number) => void
  newType: () => void
  label: string
}

export const formEditOrNew = (
  newType: PayloadActionCreator,
  editEntity: PayloadActionCreator<number>
) =>
  connect(null, dispatch => bindActionCreators({ editEntity, newType }, dispatch))(
    ({ children, editEntity, newType, label }: EditOrNewProps) => {
      const params = useParams<{ id?: string }>()
      let isEdit = false
      useEffect(() => {
        if (params.id !== null && !isNaN(Number(params.id))) {
          editEntity(Number(params.id))
          isEdit = true
        } else {
          newType()
        }
      })

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

type FormButtonProps = {
  className: string
  onClick: () => void
  Icon: () => React.ReactNode
}

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

export const GenericButton = ({ className, onClick, Icon }: FormButtonProps) => (
  <IconButton className={className} onClick={onClick}>
    <Icon />
  </IconButton>
)

type EditButtonProps = {
  className: string
  id: IdType
}

export const EditButton = ({ className, id }: EditButtonProps) => {
  const navigate = useNavigate()
  const onClick = () => navigate(`edit/${id}`)
  return <GenericButton className={className} onClick={onClick} Icon={() => <EditIcon />} />
}

type DeleteButtonProps = {
  className: string
  itemName: string
  onDelete: (id: IdType) => void
  isLoading: PayloadActionCreator<boolean>
  id: IdType
}

export const DeleteButton = ({
  className,
  id,
  isLoading,
  itemName,
  onDelete
}: DeleteButtonProps) => {
  const dispatch = useDispatch()
  const [currentId, setCurrentId] = useState<IdType | null>(null)
  const [open, setOpen] = React.useState(false)
  const [agree, setAgree] = React.useState(false)
  const onClick = () => {
    setCurrentId(id)
    setOpen(true)
  }
  const deleteId = (id: IdType) => {
    dispatch(isLoading(true))
    onDelete(id)
  }
  useEffect(() => {
    if (currentId !== null && agree) deleteId(currentId)
    setOpen(open)
  }, [agree, currentId])

  const onAgree = () => {
    setAgree(true)
  }
  const onDisagree = () => {
    setAgree(false)
    setOpen(false)
  }
  return (
    <>
      <GenericButton className={className} onClick={onClick} Icon={() => <DeleteForeverIcon />} />
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
