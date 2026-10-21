import React, { useEffect, useRef } from 'react'
import { useDispatch } from 'react-redux'
import { Button, Modal } from '@mui/material'
import FileUploadIcon from '@mui/icons-material/FileUpload'

import type { ClassName } from '../../types'
import { setError } from '../../store/root-store'
import { Loading } from '../../utils/loading'
import { setNewEntityCodeArchive, setUploading } from './actions'
import { getIsUploading, getNewEntity, useCTSelector } from './selectors'
import { DEFAULT_FILE_MATCHERS, uploadProject } from './project-upload'
import * as styles from './form.scss'

export const ProjectUpload = ({
  className,
  fileMatchers = DEFAULT_FILE_MATCHERS
}: ClassName & { fileMatchers?: readonly RegExp[] }) => {
  const dispatch = useDispatch()
  const input = useRef<HTMLInputElement>(null)
  const controller = useRef<AbortController | null>(null)
  const codeArchive = useCTSelector(s => getNewEntity(s)?.codeArchive)
  const isEditing = useCTSelector(s => getNewEntity(s)?.id !== undefined)

  useEffect(
    () => () => {
      controller.current?.abort()
    },
    []
  )

  const onChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.currentTarget.files || [])
    event.currentTarget.value = ''
    if (!files.length) return
    const request = new AbortController()
    controller.current = request
    dispatch(setUploading(true))
    try {
      const archive = await uploadProject(files, fileMatchers, request.signal)
      if (!request.signal.aborted) dispatch(setNewEntityCodeArchive(archive))
    } catch (error) {
      if (!request.signal.aborted) {
        dispatch(setError(error instanceof Error ? error.message : 'Project upload failed.'))
      }
    } finally {
      dispatch(setUploading(false))
    }
  }

  return (
    <div className={className}>
      <Button
        variant='outlined'
        startIcon={<FileUploadIcon />}
        onClick={() => input.current?.click()}
      >
        Choose code folder
      </Button>
      <input
        ref={input}
        hidden
        type='file'
        multiple
        {...{ webkitdirectory: '' }}
        onChange={onChange}
      />
    </div>
  )
}

export const ProjectUploadLoading = () => {
  const isUploading = useCTSelector(getIsUploading)
  return (
    <Modal open={isUploading} aria-label='Uploading project'>
      <div className={styles.uploading} role='status'>
        <Loading />
      </div>
    </Modal>
  )
}
