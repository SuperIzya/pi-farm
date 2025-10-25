import React from 'react'
import { getIsLoading, getNewEntity } from './selectors'
import { connect } from 'react-redux'
import {
  cancelNewEntity,
  editEntity,
  saveNewEntity,
  setLoading,
  setNewEntityDescription,
  setNewEntityDirection,
  setNewEntityImage,
  setNewEntityName,
  setNewEntityUnits,
  startNewEntity
} from './actions'
import Button from '@mui/material/Button'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import * as styles from './form.scss'
import {
  cancelButton,
  FormArgs,
  formEditOrNew,
  formMapField,
  formSaveButton,
  formTextField,
  mapSave,
  OriginalArgs,
  SaveArgs
} from '../form-mixin'
import type { PeripheryDirection } from '../../types'
import { WaitLoading } from '../../utils/wait-loading'

const textField = formTextField(getNewEntity)
const mapField = formMapField(getNewEntity)
const SaveButton = formSaveButton(getNewEntity, saveNewEntity, setLoading)
const EditOrNew = formEditOrNew(startNewEntity, editEntity)
EditOrNew.displayName = 'EditOrNew'

const Name = textField(setNewEntityName, ({ name }) => name, 'Name')

const Description = textField(
  setNewEntityDescription,
  ({ description }) => description || '',
  'Description'
)

const Units = textField(setNewEntityUnits, ({ units }) => units || '', 'Units')

const Img = connect(mapField(({ image }) => image))(
  ({ original }: OriginalArgs<string | undefined>) =>
    original !== undefined ? (
      <img src={original} alt="Periphery Type" className={styles.image} />
    ) : null
)

const imageForm = ({ save }: SaveArgs) => {
  const onSelect = (file: File) => {
    const reader = new FileReader()
    reader.onloadend = (upload) => {
      if (upload.target && upload.target.result) {
        const image = new Image()
        image.onload = () => {
          const canvas = document.createElement('canvas')
          const max_size = 300 // TODO : pull max size from a site config
          let width = image.width
          let height = image.height
          if (width > height) {
            if (width > max_size) {
              height *= max_size / width
              width = max_size
            }
          } else {
            if (height > max_size) {
              width *= max_size / height
              height = max_size
            }
          }
          canvas.width = width
          canvas.height = height
          canvas.getContext('2d')!.drawImage(image, 0, 0, width, height)
          const resizedImage = canvas.toDataURL('image/jpeg')
          save(resizedImage)
        }
        image.src = upload.target.result as string
      }
    }
    reader.readAsDataURL(file)
  }

  return (
    <div className={styles.image}>
      <Img />
      <Button variant="contained" component="label" className={styles.imageButton}>
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

const ImageSelect = connect(null, mapSave(setNewEntityImage))(imageForm)
ImageSelect.displayName = 'Image select'

const isPeripheryDirection = (value: string): value is PeripheryDirection =>
  value === 'in' || value === 'out' || value === 'both'

const directionForm = ({ original, save }: FormArgs<PeripheryDirection | undefined>) => (
  <div className={styles.direction}>
    <InputLabel id="direction-label">Direction</InputLabel>
    <Select
      labelId={'direction-label'}
      id={'direction'}
      label={'Direction'}
      value={original || ''}
      onChange={(e) => isPeripheryDirection(e.target.value) && save(e.target.value)}
    >
      <MenuItem value={''}>Select direction</MenuItem>
      <MenuItem value={'in'}>Into controller</MenuItem>
      <MenuItem value={'out'}>Out of controller</MenuItem>
      <MenuItem value={'both'}>Both</MenuItem>
    </Select>
  </div>
)

const Direction = connect(
  mapField(({ direction }) => direction),
  mapSave(setNewEntityDirection)
)(directionForm)
Direction.displayName = 'Direction'

const CancelButton = cancelButton(cancelNewEntity)

export const InnerForm = () => (
  <div className={styles.container}>
    <WaitLoading isLoadingSelector={getIsLoading}>
      <EditOrNew label={'Periphery Type'}>
        <Name className={styles.name} />
        <Direction />
        <Units className={styles.units} />
        <Description className={styles.description} multiline={true} />
        <ImageSelect />
        <SaveButton className={styles.save} />
        <CancelButton className={styles.cancel} />
      </EditOrNew>
    </WaitLoading>
  </div>
)
