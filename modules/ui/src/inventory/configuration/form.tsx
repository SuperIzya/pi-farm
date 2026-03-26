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
import { GraphForm } from './graph-form'
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
  <div className={styles.container}>
    <WaitLoading isLoadingSelector={isLoadingSelector}>
      <EditOrNew label={'Configuration'}>
        <Name className={styles.name} />
        <Description multiline={true} className={styles.description} />
        <div className={styles.graph}>
          <GraphForm />
        </div>
        <div className={styles.buttons}>
          <SaveButton className={styles.save} />
          <CancelButton className={styles.cancel} />
        </div>
      </EditOrNew>
    </WaitLoading>
  </div>
)
