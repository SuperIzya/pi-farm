import React from 'react'
import * as styles from './form.scss'
import {
  cancelNewEntity,
  editEntity,
  saveNewEntity,
  setLoading,
  setNewEntityDescription,
  setNewEntityName,
  setNewEntityTypeId,
  startNewEntity
} from './actions'
import Select, { SelectChangeEvent } from '@mui/material/Select'
import MenuItem from '@mui/material/MenuItem'
import { getKnownEntities as getKnownControllerTypes } from '../controller-types/selectors'
import { cancelButton, formEditOrNew, formSaveButton, formTextField } from '../form-mixin'
import { getIsLoading, getNewEntity } from './selectors'
import { Guard } from '../periphery-types/guard'
import { WaitLoading } from '../../utils/wait-loading'
import { createSelector } from 'reselect'
import { IdType } from '../../types'
import { useDispatch, useSelector } from 'react-redux'
import { RootState } from '../controller-types/types'

const textField = formTextField(getNewEntity)
const SaveButton = formSaveButton(getNewEntity, saveNewEntity, setLoading)
const CancelButton = cancelButton(cancelNewEntity)
const EditOrNew = formEditOrNew(startNewEntity, editEntity)

const Name = textField(setNewEntityName, ({ name }) => name || '', 'Name')

const Description = textField(
  setNewEntityDescription,
  ({ description }) => description || '',
  'Description'
)

const listSelector = createSelector(getNewEntity, getKnownControllerTypes, (entity, types) => ({
  typeId: entity?.typeId,
  types
}))

const TypeForm = () => {
  const { typeId, types } = useSelector.withTypes<RootState>()(listSelector)
  const dispatch = useDispatch()
  const onSelected = (typeId: IdType) => dispatch(setNewEntityTypeId(typeId))
  const onChange = (e: SelectChangeEvent<IdType>) => onSelected(e.target.value)
  return (
    <Select
      label={'Controller type'}
      onChange={onChange}
      value={typeId || ''}
      className={styles.plist}
    >
      {types.map(({ id, name }) => (
        <MenuItem key={id} value={id}>
          {name}
        </MenuItem>
      ))}
    </Select>
  )
}
export const InnerForm = () => (
  <div className={styles.container}>
    <Guard />
    <WaitLoading isLoadingSelector={getIsLoading}>
      <EditOrNew label={'Controller'}>
        <Name className={styles.name} />
        <Description className={styles.description} multiline={true} />
        <TypeForm />
        <SaveButton className={styles.save} />
        <CancelButton className={styles.cancel} />
      </EditOrNew>
    </WaitLoading>
  </div>
)
