import React from 'react'
import { PeripheryDirection } from './types'
import { getNewType } from './selectors'
import { connect } from 'react-redux'
import {
  cancelNewType,
  saveNewType,
  setNewTypeDescription,
  setNewTypeDirection,
  setNewTypeName,
  setNewTypePicture,
  setNewTypeUnits
} from './actions'
import Button from '@mui/material/Button'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import styles from './form.scss'
import {
  cancelButton,
  createFormRoutines,
  FormArgs,
  mapSave,
  OriginalArgs,
  SaveArgs
} from '../form'

const { textForm, mapField, saveButton } = createFormRoutines(getNewType)

const Name = textForm(setNewTypeName, ({ name }) => name || '')

const Description = textForm(
  setNewTypeDescription,
  ({ description }) => description || ''
)

const Units = textForm(setNewTypeUnits, ({ units }) => units || '')

const Img = connect(mapField(({ picture }) => picture || ''))(
  ({ original }: OriginalArgs) => (
    <img src={original} alt="Periphery Type" style={{ maxWidth: '100%' }} />
  )
)

const imageForm = ({ save }: SaveArgs) => {
  const onSelect = (file: File) => {
    const reader = new FileReader()
    reader.onloadend = (upload) => {
      if (upload.target && upload.target.result) {
        save(`data:image/gif;base64,${upload.target.result as string}`)
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

const Image = connect(
  mapField(({ picture }) => picture || ''),
  mapSave(setNewTypePicture)
)(imageForm)

const directionForm = ({ original, save }: FormArgs<PeripheryDirection>) => (
  <>
    <InputLabel id="direction-label">Direction</InputLabel>
    <Select
      labelId={'direction-label'}
      id="direction"
      label="Direction"
      value={original}
      onChange={(e) => save(e.target.value as PeripheryDirection)}
    >
      <MenuItem value={'in'}>In</MenuItem>
      <MenuItem value={'out'}>Out</MenuItem>
      <MenuItem value={'both'}>Both</MenuItem>
    </Select>
  </>
)

const Direction = connect(
  mapField(({ direction }) => direction || 'in'),
  mapSave(setNewTypeDirection)
)(directionForm)
const SaveButton = saveButton(saveNewType)
const CancelButton = cancelButton(cancelNewType)

export const PeripheryTypeForm = () => (
  <div className={styles.container}>
    <Image />
    <Name />
    <Direction />
    <Units />
    <Description />
    <SaveButton />
    <CancelButton />
  </div>
)
