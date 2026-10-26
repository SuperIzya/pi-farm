import React, { useEffect, useRef } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Button, Modal, Tooltip } from '@mui/material'
import FileUploadIcon from '@mui/icons-material/FileUpload'

import type { ClassName } from '../../types'
import { getAppConfiguration, setError } from '../../store/root-store'
import type { RootState } from '../../store/root-store'
import { Loading } from '../../utils/loading'
import { setNewEntityCodeArchive, setUploading } from './actions'
import { getIsUploading, useCTSelector } from './selectors'
import { DEFAULT_FILE_MATCHERS, uploadProject } from './project-upload'
import * as styles from './form.scss'

const useAppSelector = useSelector.withTypes<RootState>()

export const ProjectUpload = ({
  className,
  fileMatchers = DEFAULT_FILE_MATCHERS
}: ClassName & { fileMatchers?: readonly RegExp[] }) => {
  const dispatch = useDispatch()
  const codeFileGlobs = useAppSelector(getAppConfiguration)?.codeFileGlobs
  const input = useRef<HTMLInputElement>(null)
  const controller = useRef<AbortController | null>(null)

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
      if (!codeFileGlobs) throw new Error('Code file patterns are not available yet.')
      const archive = await uploadProject(files, codeFileGlobs, fileMatchers, request.signal)
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
      <Tooltip
        title={
          codeFileGlobs
            ? `Included files: ${codeFileGlobs.join(', ')}`
            : 'Loading code file patterns.'
        }
      >
        <span>
          <Button
            variant='outlined'
            startIcon={<FileUploadIcon />}
            disabled={!codeFileGlobs}
            onClick={() => input.current?.click()}
          >
            Choose code folder
          </Button>
        </span>
      </Tooltip>
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
