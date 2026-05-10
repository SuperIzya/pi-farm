import React from 'react'
import { cancelButton, formEditOrNew, formSaveButton, formTextField } from '../form-mixin'
import { getIsLoading, getNewEntity, getProcessingUnitsIsLoading } from './selectors'
import {
  cancelNewEntity,
  editEntity,
  saveNewEntity,
  setDescription,
  setLoading,
  setName,
  startNewEntity
} from './actions'
import { WaitLoading } from '../../utils/wait-loading'
import { Graph } from './graph/graph-form'
import * as styles from './form.scss'
import { createSelector } from 'reselect'

const textField = formTextField(getNewEntity)
const SaveButton = formSaveButton(getNewEntity, saveNewEntity, setLoading)
const CancelButton = cancelButton(cancelNewEntity)
const EditOrNew = formEditOrNew(startNewEntity, editEntity)

const Name = textField(setName, ({ name }) => name, 'Name')

const Description = textField(setDescription, ({ description }) => description || '', 'Description')

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
          <Graph />
        </div>
        <div className={styles.buttons}>
          <SaveButton className={styles.save} />
          <CancelButton className={styles.cancel} />
        </div>
      </EditOrNew>
    </WaitLoading>
  </div>
)
