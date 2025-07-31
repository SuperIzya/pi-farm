import React, { Dispatch, useState } from 'react'
import { Action, PayloadAction, PayloadActionCreator } from '@reduxjs/toolkit'
import { connect } from 'react-redux'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import { NewType } from './types'
import type { ItemProps } from '../../utils/list'
import { redirect } from 'react-router'

export type OriginalArgs<T = string> = { original: T }
export type SaveArgs<T = string> = { save: (value: T) => void }

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

const textForm = <S, T>(
  creator: PayloadActionCreator<string>,
  objExtractor: ObjExtractor<S, T>,
  fieldExtractor: FieldExtractor<T, string>
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
        label="Requeired*"
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
      save: () => dispatch(saveNewType())
    })
  )(({ save, canBeSaved }: { save: () => void; canBeSaved: boolean }) => (
    <Button variant="contained" color="primary" onClick={save} disabled={!canBeSaved}>
      Save
    </Button>
  ))

export const cancelButton = (cancelNewType: PayloadActionCreator) =>
  connect(null, (dispatch: Dispatch<PayloadAction>) => ({
    cancel: () => {
      dispatch(cancelNewType())
      history.back()
    }
  }))(({ cancel }: { cancel: () => void }) => (
    <Button variant="outlined" color="secondary" onClick={cancel}>
      Cancel
    </Button>
  ))

export const createFormRoutines = <T, S>(objectExtractor: ObjExtractor<S, T>) => ({
  textForm: (
    creator: PayloadActionCreator<string>,
    fieldExtractor: FieldExtractor<T, string>
  ) => textForm(creator, objectExtractor, fieldExtractor),
  saveButton: (saveNewType: PayloadActionCreator) =>
    saveButton(objectExtractor, saveNewType),
  mapField: function <Q>(fieldExtractor: FieldExtractor<T, Q>) {
    return mapField(objectExtractor, fieldExtractor)
  }
})

export const editButton = <S, T extends { id: number }>(
  className: string,
  editType: PayloadActionCreator<number>,
  objectsExtractor: KnownObjectsExtractor<S, T>
) => {
  type EditButtonProps = {
    onClick: () => void
  }

  const mapEditButtonDispatch =
    () =>
    (dispatch: Dispatch<Action>, { id }: { id: number }) => ({
      onClick: () => {
        dispatch(editType(id))
        redirect(`edit/${id}`)
      }
    })

  const mapId =
    () =>
    (state: S, { key }: ItemProps) => ({
      id: objectsExtractor(state)[key].id
    })

  return connect(mapId)(
    connect(
      null,
      mapEditButtonDispatch
    )(({ onClick }: EditButtonProps) => (
      <Button className={className} onClick={onClick}>
        Edit
      </Button>
    ))
  )
}
