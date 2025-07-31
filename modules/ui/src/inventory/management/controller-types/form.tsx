import React, { Dispatch } from 'react'
import styles from './form.scss'
import { getNewType } from './selectors'
import {
  cancelNewType,
  saveNewType,
  setNewTypeDescription,
  setNewTypeName,
  setNewTypeSchema,
  setNewTypeCode,
  addNewTypePeriphery
} from './actions'
import Button from '@mui/material/Button'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import {
  cancelButton,
  createFormRoutines,
  FormArgs,
  mapSave,
  OriginalArgs,
  SaveArgs
} from '../form'
import { Action } from '@reduxjs/toolkit'
import { RootState } from './types'
import { PeripheryForm } from './periphery-form'

const { textForm, mapField, saveButton } = createFormRoutines(getNewType)

const Name = textForm(setNewTypeName, ({ name }) => name || '')

const Description = textForm(
  setNewTypeDescription,
  ({ description }) => description || ''
)

const Code = textForm(setNewTypeCode, ({ code }) => code || '')

const SaveButton = saveButton(saveNewType)
const CancelButton = cancelButton(cancelNewType)

export const ControllerTypeForm = () => (
  <div className={styles.container}>
    <Name />
    <Description />
    <PeripheryForm />
    <Code />
    <SaveButton />
    <CancelButton />
  </div>
)
