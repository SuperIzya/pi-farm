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
  startNewType,
  setLoading
} from './actions'
import { cancelButton, formEditOrNew, formSaveButton, formTextField } from '../form-mixin'
import { PeripheryForm } from './periphery-form'

const textField = formTextField(getNewType)
const SaveButton = formSaveButton(getNewType, saveNewType, setLoading)
const EditOrNew = formEditOrNew(startNewType, editType)

const Name = textField(setNewTypeName, ({ name }) => name, 'Name')

const Description = textField(
  setNewTypeDescription,
  ({ description }) => description || '',
  'Description'
)

const Code = textField(setNewTypeCode, ({ code }) => code || '', 'Code')

const CancelButton = cancelButton(cancelNewType)

export const ControllerTypeForm = () => (
  <div className={styles.container}>
    <EditOrNew label={'Controller Type'}>
      <Name className={styles.name} />
      <Description className={styles.description} multiline={true} />
      <PeripheryForm className={styles.peripheryForm} />
      <Code className={styles.code} />
      <SaveButton className={styles.save} />
      <CancelButton className={styles.cancel} />
    </EditOrNew>
  </div>
)
