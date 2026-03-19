import React from 'react'
import { cancelButton, formEditOrNew, formSaveButton, formTextField } from '../form-mixin'
import { getIsLoading, getNewEntity, getProcessingUnitsIsLoading } from './selectors'
import {
  cancelNewEntity,
  editEntity,
  saveNewEntity,
  setLoading,
  startNewEntity,
  setNewEntityName,
  setNewEntityDescription
} from './actions'
import { WaitLoading } from '../../utils/wait-loading'
import * as styles from './form.scss'
import { createSelector } from 'reselect'

const textField = formTextField(getNewEntity)
const SaveButton = formSaveButton(getNewEntity, saveNewEntity, setLoading)
const CancelButton = cancelButton(cancelNewEntity)
const EditOrNew = formEditOrNew(startNewEntity, editEntity)

const Name = textField(setNewEntityName, ({ name }) => name, 'Name')

const Description = textField(
  setNewEntityDescription,
  ({ description }) => description || '',
  'Description'
)

const isLoadingSelector = createSelector(
  [getIsLoading, getProcessingUnitsIsLoading],
  (a, b) => a || b
)

export const InnerForm = () => (
  <WaitLoading isLoadingSelector={isLoadingSelector}>
    <EditOrNew label={'Configuration'}>
      <Name className={styles.name} />
      <Description multiline={true} className={styles.description} />
      <SaveButton className={styles.save} />
      <CancelButton className={styles.cancel} />
    </EditOrNew>
  </WaitLoading>
)
