import React from 'react'
import * as styles from './form.scss'
import { getIsLoading, getNewEntity } from './selectors'
import {
  cancelNewEntity,
  saveNewEntity,
  setNewEntityDescription,
  setNewEntityName,
  setNewEntityCode,
  editEntity,
  startNewEntity,
  setLoading
} from './actions'
import { cancelButton, formEditOrNew, formSaveButton, formTextField } from '../form-mixin'
import { PeripheryForm } from './periphery-form'
import { Guard } from '../periphery-types/guard'
import { WaitLoading } from '../../utils/wait-loading'

const textField = formTextField(getNewEntity)
const SaveButton = formSaveButton(getNewEntity, saveNewEntity, setLoading)
const EditOrNew = formEditOrNew(startNewEntity, editEntity)

const Name = textField(setNewEntityName, ({ name }) => name, 'Name')

const Description = textField(
  setNewEntityDescription,
  ({ description }) => description || '',
  'Description'
)

const Code = textField(setNewEntityCode, ({ code }) => code || '', 'Code')

const CancelButton = cancelButton(cancelNewEntity)

export const ControllerTypeForm = () => (
  <div className={styles.container}>
    <Guard />
    <WaitLoading isLoadingSelector={getIsLoading}>
      <EditOrNew label={'Controller Type'}>
        <Name className={styles.name} />
        <Description className={styles.description} multiline={true} />
        <PeripheryForm className={styles.peripheryForm} />
        <Code className={styles.code} />
        <SaveButton className={styles.save} />
        <CancelButton className={styles.cancel} />
      </EditOrNew>
    </WaitLoading>
  </div>
)
