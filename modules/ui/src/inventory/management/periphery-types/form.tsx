import React from 'react'
import { getNewType } from './selectors'
import { connect } from 'react-redux'
import {
  cancelNewType,
  editType,
  saveNewType,
  setNewTypeDescription,
  setNewTypeDirection,
  setNewTypeName,
  setNewTypeImage,
  setNewTypeUnits,
  startNewType
} from './actions'
import Button from '@mui/material/Button'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import * as styles from './form.scss'
import {
  cancelButton,
  createFormRoutines,
  FormArgs,
  mapSave,
  OriginalArgs,
  SaveArgs
} from '../form'
import type { PeripheryDirection } from '../../../types'

const { textField, mapField, saveButton, EditOrNew } = createFormRoutines(
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

const Units = textField(setNewTypeUnits, ({ units }) => units || '', 'Units')

const Img = connect(mapField(({ image }) => image))(
  ({ original }: OriginalArgs<string | undefined>) =>
    original !== undefined ? (
      <img src={original} alt="Periphery Type" style={{ maxWidth: '100%' }} />
    ) : null
)

const imageForm = ({ save }: SaveArgs) => {
  const onSelect = (file: File) => {
    const reader = new FileReader()
    reader.onloadend = (upload) => {
      if (upload.target && upload.target.result) {
        save(upload.target.result as string)
      }
    }
    reader.readAsDataURL(file)
  }

  return (
    <div>
      <Img />
      <Button variant="contained" component="label">
        Upload File
        <input
          type="file"
          hidden
          onChange={(e) => {
            if (e.target.files && e.target.files[0]) {
              onSelect(e.target.files[0])
            }
          }}
        />
      </Button>
    </div>
  )
}

const Image = connect(null, mapSave(setNewTypeImage))(imageForm)

const directionForm = ({ original, save }: FormArgs<PeripheryDirection | undefined>) => (
  <>
    <InputLabel id="direction-label">Direction</InputLabel>
    <Select
      labelId={'direction-label'}
      id="direction"
      label="Direction"
      value={original}
      onChange={(e) => save(e.target.value as PeripheryDirection)}
    >
      <MenuItem value={undefined}>Select direction</MenuItem>
      <MenuItem value={'in'}>In</MenuItem>
      <MenuItem value={'out'}>Out</MenuItem>
      <MenuItem value={'both'}>Both</MenuItem>
    </Select>
  </>
)

const Direction = connect(
  mapField(({ direction }) => direction),
  mapSave(setNewTypeDirection)
)(directionForm)

const SaveButton = saveButton(saveNewType)
const CancelButton = cancelButton(cancelNewType)

export const PeripheryTypeForm = () => (
  <div className={styles.container}>
    <EditOrNew label={'Periphery Type'}>
      <Name />
      <Direction />
      <Units />
      <Description />
      <Image />
      <SaveButton />
      <CancelButton />
    </EditOrNew>
  </div>
)
