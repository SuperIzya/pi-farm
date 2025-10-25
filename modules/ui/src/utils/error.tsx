import React from 'react'

import { setError, clearError, getError } from '../store/root-store'
import { useOnReceiveData } from '../client'
import { connect } from 'react-redux'
import { createSelector } from 'reselect'
import * as styles from './error.scss'
import Modal from '@mui/material/Modal'
import Alert from '@mui/material/Alert'

const errorSelector = createSelector(getError, (error) => ({ error, open: !!error }))

type ErrorProps = {
  error: string | undefined
  open: boolean
  onClose: () => void
}

const ErrorDialog = connect(errorSelector, (dispatch) => ({
  onClose: () => dispatch(clearError())
}))(({ error, onClose, open }: ErrorProps) => (
  <Modal open={open} onClose={onClose}>
    <div className={styles.container}>
      <div className={styles.content}>
        <div className={styles.title}>
          <Alert severity={'error'} variant={'filled'}>
            Server error
          </Alert>
        </div>
        <div className={styles.body}>
          <Alert severity={'error'}>{error}</Alert>
        </div>
      </div>
    </div>
  </Modal>
))
export const Error = () => {
  const receive = useOnReceiveData()
  receive('error', setError)
  return <ErrorDialog />
}
