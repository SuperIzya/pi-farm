import React, { Dispatch, useState } from 'react'
import { bindActionCreators, PayloadAction, PayloadActionCreator } from '@reduxjs/toolkit'
import { connect, useDispatch } from 'react-redux'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import { NewType } from './types'
import type { ItemProps } from '../../utils/list'
import { redirect, useNavigate, useParams } from 'react-router'

export type OriginalArgs<T = string> = { original: T | undefined }
export type SaveArgs<T = string> = { save: (value: T | undefined) => void }

export type FormArgs<T = string> = OriginalArgs<T> & SaveArgs<T>

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

const textField = <S, T>(
  creator: PayloadActionCreator<string | undefined>,
  objExtractor: ObjExtractor<S, T>,
  fieldExtractor: FieldExtractor<T, string | undefined>,
  label: string
) =>
  connect(
    mapField(objExtractor, fieldExtractor),
    mapSave(creator)
  )(({ original, save }: FormArgs) => {
    const [name, setName] = useState(original)

    return (
      <TextField
        required
        id="outlined-required"
        label={label}
        variant="outlined"
        onChange={(e) => setName(e.target.value)}
        onBlur={() => save(name)}
        value={name}
      />
    )
  })

const saveButton = <S, T>(
  objExtractor: ObjExtractor<S, NewType<T> | undefined>,
  saveNewType: PayloadActionCreator
) =>
  connect(
    (state: S) => ({
      canBeSaved: objExtractor(state)?.canBeSaved || false
    }),
    (dispatch: Dispatch<PayloadAction>) => ({
      save: () => {
        dispatch(saveNewType())
        redirect('..')
      }
    })
  )(({ save, canBeSaved }: { save: () => void; canBeSaved: boolean }) => (
    <Button variant="contained" color="primary" onClick={save} disabled={!canBeSaved}>
      Save
    </Button>
  ))

export const cancelButton = (cancelNewType: PayloadActionCreator) => () => {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const cancel = () => {
    dispatch(cancelNewType())
    navigate('..')
  }

  return (
    <Button variant="outlined" color="secondary" onClick={cancel}>
      Cancel
    </Button>
  )
}

export const createFormRoutines = <T, S>(
  objectExtractor: ObjExtractor<S, T>,
  newType: PayloadActionCreator,
  editType: PayloadActionCreator<number>
) => ({
  textField: (
    creator: PayloadActionCreator<string | undefined>,
    fieldExtractor: FieldExtractor<T, string | undefined>,
    label: string
  ) => textField(creator, objectExtractor, fieldExtractor, label),
  saveButton: (saveNewType: PayloadActionCreator) =>
    saveButton(objectExtractor, saveNewType),
  mapField: function <Q>(fieldExtractor: FieldExtractor<T, Q>) {
    return mapField(objectExtractor, fieldExtractor)
  },
  EditOrNew: connect(null, (dispatch) =>
    bindActionCreators({ editType, newType }, dispatch)
  )(
    ({
      children,
      editType,
      newType,
      label
    }: {
      children: React.ReactNode
      editType: (id: number) => void
      newType: () => void
      label: string
    }) => {
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
})

type EditButtonProps<S, T extends { id: number }> = {
  className: string
  objectsExtractor: KnownObjectsExtractor<S, T>
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

export const EditButton = <S, T extends { id: number }>({
  className,
  objectsExtractor,
  key
}: EditButtonProps<S, T>) => {
  const mapId = () => (state: S) => ({
    id: objectsExtractor(state)[key].id
  })

  const navigate = useNavigate()

  const C = connect(mapId)(({ id }: { id: number }) => (
    <Button className={className} onClick={() => navigate(`edit/${id}`)}>
      Edit
    </Button>
  ))

  return <C />
}
