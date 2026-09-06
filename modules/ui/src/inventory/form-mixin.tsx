import React, { useState, useEffect, Dispatch } from 'react'
import {
  ActionCreatorWithOptionalPayload,
  bindActionCreators,
  PayloadAction,
  PayloadActionCreator
} from '@reduxjs/toolkit'
import { useDispatch, useSelector } from 'react-redux'
import Button from '@mui/material/Button'
import type { NewEntity, IdType, ClassName } from '../types'
import { useNavigate, useParams } from 'react-router'
import IconButton from '@mui/material/IconButton'
import EditIcon from '@mui/icons-material/Edit'
import TextField, { TextFieldProps } from '@mui/material/TextField'
import DeleteForeverIcon from '@mui/icons-material/DeleteForever'
import * as styles from './form-mixin.scss'
import ThumbUpIcon from '@mui/icons-material/ThumbUp'
import ThumbDownIcon from '@mui/icons-material/ThumbDown'
import Input, { InputProps } from '@mui/material/Input'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogActions from '@mui/material/DialogActions'

export type OriginalArgs<T = string> = { original: T | undefined }
export type SaveArgs<T = string> = { save: ActionCreatorWithOptionalPayload<T | undefined> }

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export type FormArgs<T = string, P = InputProps | TextFieldProps | {}> = OriginalArgs<T>
  & SaveArgs<T>
  & ClassName
  & P

type ObjExtractor<S, T> = (s: S) => Partial<T> | undefined
type FieldExtractor<T, Q> = (p: Partial<T>) => Q

const mapFieldValue =
  <S, T, Out>(objExtractor: ObjExtractor<S, T>, fieldExtractor: FieldExtractor<T, Out>) =>
  (state: S) =>
    fieldExtractor(objExtractor(state) || {})

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

export const formInput =
  <S, T, F, P extends object>(
    objExtractor: ObjExtractor<S, T>,
    creator: ActionCreatorWithOptionalPayload<F | undefined>,
    fieldExtractor: FieldExtractor<T, F>,
    component: React.ComponentType<FormArgs<F, P>>
  ) =>
  (args: ClassName & P) => {
    const { original } = useSelector(mapField(objExtractor, fieldExtractor))
    const dispatch = useDispatch()
    const save = bindActionCreators(creator, dispatch)
    return React.createElement(component, { ...args, original, save })
  }

export const formTextInput =
  <S, T>(objExtractor: ObjExtractor<S, T>) =>
  (label: string) =>
  <K extends string = string>(
    creator: ActionCreatorWithOptionalPayload<string | undefined, K>,
    fieldExtractor: FieldExtractor<T, string>
  ) =>
    formInput(
      objExtractor,
      creator,
      fieldExtractor,
      ({ original, save, className, ...props }: FormArgs<string, InputProps>) => {
        const [name, setName] = useState(original)

        useEffect(() => setName(original), [original])
        return (
          <Input
            required
            {...props}
            id='outlined-required'
            placeholder={label}
            className={className}
            onChange={e => setName(e.target.value)}
            onBlur={() => save(name)}
            value={name || ''}
          />
        )
      }
    )

export const formTextField =
  <S, T>(objExtractor: ObjExtractor<S, T>) =>
  <K extends string = string>(
    creator: ActionCreatorWithOptionalPayload<string | undefined, K>,
    fieldExtractor: FieldExtractor<T, string>,
    label: string
  ) =>
    formInput(
      objExtractor,
      creator,
      fieldExtractor,
      ({ original, save, className, ...props }: FormArgs<string, TextFieldProps>) => {
        const [name, setName] = useState(original)

        useEffect(() => setName(original), [original])
        return (
          <TextField
            {...props}
            required
            id='outlined-required'
            label={label}
            className={className}
            onChange={e => setName(e.target.value)}
            onBlur={() => save(name)}
            value={name || ''}
          />
        )
      }
    )

export const formSaveButton =
  <S, T>(
    objExtractor: ObjExtractor<S, NewEntity<T> | undefined>,
    saveNewEntity: PayloadActionCreator,
    setLoading: PayloadActionCreator<boolean>
  ) =>
  ({ className }: ClassName) => {
    const canBeSavedSelector = (s: S) => objExtractor(s)?.canBeSaved || false
    const canBeSaved = useSelector(canBeSavedSelector)
    const navigate = useNavigate()
    const p = bindActionCreators({ saveNewEntity, setLoading }, useDispatch())
    const onClick = () => {
      if (!canBeSaved) return
      p.setLoading(true)
      p.saveNewEntity()
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

export const formMapFieldValue =
  <S, T>(objectExtractor: ObjExtractor<S, T>) =>
  <Out,>(fieldExtractor: FieldExtractor<T, Out>) =>
    mapFieldValue(objectExtractor, fieldExtractor)

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

export const formEditOrNew =
  (newType: PayloadActionCreator, editEntity: PayloadActionCreator<number>) =>
  ({ children, label }: Omit<EditOrNewProps, 'editEntity' | 'newType'>) => {
    const actions = bindActionCreators({ editEntity, newType }, useDispatch())
    const params = useParams<{ id?: string }>()
    let isEdit = false
    useEffect(() => {
      if (params.id !== null && !isNaN(Number(params.id))) {
        actions.editEntity(Number(params.id))
        isEdit = true
      } else {
        actions.newType()
      }
    }, [params.id, actions])

    return (
      <>
        <h3>
          {isEdit ? 'Edit' : 'New'} {label}
        </h3>
        {children}
      </>
    )
  }

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
  const [open, setOpen] = useState(false)
  const [agree, setAgree] = useState(false)
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
