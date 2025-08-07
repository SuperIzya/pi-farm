import React from 'react'
import * as styles from './form.scss'
import { getNewType } from './selectors'
import {
  cancelNewType,
  saveNewType,
  setNewTypeDescription,
  setNewTypeName,
  setNewTypeCode,
  editType,
  startNewType
} from './actions'
import { cancelButton, createFormRoutines } from '../form'
import { PeripheryForm } from './periphery-form'

const { textField, saveButton, EditOrNew } = createFormRoutines(
  getNewType,
  startNewType,
  editType
)

const Name = textField(setNewTypeName, ({ name }) => name, 'Name')

const Description = textField(
  setNewTypeDescription,
  ({ description }) => description || '',
  'Description'
)

const Code = textField(setNewTypeCode, ({ code }) => code || '', 'Code')

const SaveButton = saveButton(saveNewType)
const CancelButton = cancelButton(cancelNewType)

export const ControllerTypeForm = () => (
  <div className={styles.container}>
    <EditOrNew label={'Controller Type'}>
      <Name />
      <Description />
      <PeripheryForm />
      <Code />
      <SaveButton />
      <CancelButton />
    </EditOrNew>
  </div>
)
