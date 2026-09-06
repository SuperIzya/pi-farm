import React from 'react'
import { getIsLoading, getNewEntity } from './selectors'
import { useDispatch, useSelector } from 'react-redux'
import {
  cancelNewEntity,
  editEntity,
  saveNewEntity,
  setLoading,
  setNewEntityDescription,
  setNewEntityImage,
  setNewEntityName,
  startNewEntity
} from './actions'
import Button from '@mui/material/Button'
import * as styles from './form.scss'
import {
  cancelButton,
  formEditOrNew,
  formMapFieldValue,
  formSaveButton,
  formTextField,
  SaveArgs
} from '../form-mixin'
import { WaitLoading } from '../../utils/wait-loading'
import { NewEntityConnectionsList } from './connections'
import { bindActionCreators } from '@reduxjs/toolkit'
import { ImportData } from '../page'

const textField = formTextField(getNewEntity)
const mapFieldValue = formMapFieldValue(getNewEntity)
const SaveButton = formSaveButton(getNewEntity, saveNewEntity, setLoading)
const EditOrNew = formEditOrNew(startNewEntity, editEntity)

const Name = textField(setNewEntityName, ({ name }) => name || '', 'Name')

const Description = textField(
  setNewEntityDescription,
  ({ description }) => description || '',
  'Description'
)

const CancelButton = cancelButton(cancelNewEntity)

const ImageForm = ({ save }: SaveArgs) => {
  const image = useSelector(mapFieldValue(({ image }) => image))
  const onSelect = (file: File) => {
    const reader = new FileReader()
    reader.onloadend = upload => {
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

  const Btn = () => (
    <Button variant='contained' component='label' className={styles.imageButton}>
      Upload File
      <input
        type='file'
        hidden
        onChange={e => {
          if (e.target.files && e.target.files[0]) {
            onSelect(e.target.files[0])
          }
        }}
      />
    </Button>
  )

  const Img = () => <img src={image} alt='Periphery Type' className={styles.image} />

  return <div className={styles.image}>{image ? <Img /> : <Btn />}</div>
}

const ImageSelect = () => {
  const dispatch = useDispatch()
  const save = bindActionCreators(setNewEntityImage, dispatch)
  return <ImageForm save={save} />
}
ImageSelect.displayName = 'Image select'

export const InnerForm = () => (
  <div className={styles.container}>
    <WaitLoading isLoadingSelector={getIsLoading}>
      <EditOrNew label={'Periphery Type'}>
        <Name className={styles.name} />
        <ImportData className={styles.import} />
        <ImageSelect />
        <NewEntityConnectionsList />
        <Description className={styles.description} multiline={true} />
        <SaveButton className={styles.save} />
        <CancelButton className={styles.cancel} />
      </EditOrNew>
    </WaitLoading>
  </div>
)
