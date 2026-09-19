import React from 'react'

import { setError, clearError, getError } from '../store/root-store'
import { useOnReceiveData } from '../client'
import { useDispatch, useSelector } from 'react-redux'
import * as styles from './error.scss'
import Modal from '@mui/material/Modal'
import Alert from '@mui/material/Alert'

const ErrorDialog = () => {
  const error = useSelector(getError)
  const dispatch = useDispatch()
  const onClose = () => dispatch(clearError())
  return (
    <Modal open={!!error} onClose={onClose}>
      <div className={styles.container}>
        <div className={styles.title}>
          <Alert severity={'error'} variant={'filled'}>
            Server error
          </Alert>
        </div>
        <div className={styles.body}>
          <Alert severity={'error'}>{error}</Alert>
        </div>
      </div>
    </Modal>
  )
}
export const Error = () => {
  const receive = useOnReceiveData()
  receive('error', setError)
  return <ErrorDialog />
}
