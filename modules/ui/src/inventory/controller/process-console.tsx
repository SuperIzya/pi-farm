import React, { useEffect, useRef } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { IconButton, Tooltip } from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import TerminalIcon from '@mui/icons-material/Terminal'
import { AppWebSocket } from '../../client'
import {
  closeConsole,
  getIsOpen,
  getLogs,
  getProcess,
  openConsole,
  receiveProcessLog,
  receiveProcessState
} from './store'
import type { RootState } from './types'
import * as styles from './process-console.scss'
import type { ProcessLog } from '../../types'

const useCSelector = useSelector.withTypes<RootState>()

const ProcessStatus = () => {
  const process = useCSelector(getProcess)
  return process ? (
    <span className={styles.status} role='status'>
      {process.controllerId !== undefined && `Controller ${process.controllerId}: `}
      {process.status}
      {process.exitCode !== undefined && ` (exit ${process.exitCode})`}
      {process.error && `: ${process.error}`}
    </span>
  ) : null
}

const Stdout = ({ message }: { message: string }) => (
  <span className={styles.stdout}>
    {message}
  </span>
)
const Stderr = ({ message }: { message: string }) => (
  <span className={styles.stderr}>
    {message}
  </span>
)

const ShowLog = ({ log }: { log: ProcessLog }) => (
  <>
  {'stderr' in log ? (
    <Stderr message={log.stderr.message} />
  ) : (
    <Stdout message={log.stdout.message} />
  )}
  {'\n'}
  </>
)

const ProcessLogs = () => {
  const logs = useCSelector(getLogs)
  const output = useRef<HTMLPreElement>(null)
  useEffect(() => {
    output.current?.scrollTo({ top: output.current.scrollHeight })
  }, [logs])
  return (
    <pre ref={output} className={styles.output} aria-label='Process output' tabIndex={0}>
      {logs.map((log, index) => <ShowLog log={log} key={index} />)}
    </pre>
  )
}

const ConsolePanel = () => {
  const dispatch = useDispatch()
  return (
    <section className={styles.panel} aria-label='Process console'>
      <header className={styles.header}>
        <TerminalIcon fontSize='small' />
        <h2>Process Console</h2>
        <ProcessStatus />
        <Tooltip title='Close console'>
          <IconButton aria-label='Close console' onClick={() => dispatch(closeConsole())}>
            <CloseIcon />
          </IconButton>
        </Tooltip>
      </header>
      <ProcessLogs />
    </section>
  )
}

export const ProcessConsole = () => {
  const isOpen = useCSelector(getIsOpen)
  const dispatch = useDispatch()
  const send = AppWebSocket.useSendCommand()
  const onReceive = AppWebSocket.useOnReceiveData()
  onReceive('process-logs', receiveProcessLog)
  onReceive('process-state', receiveProcessState)
  useEffect(() => {
    if (!isOpen) return
    send('subscribe-process-logs')
    send('get-process-state')
    return () => {
      send('unsubscribe-process-logs')
      dispatch(closeConsole())
    }
  }, [isOpen, send, dispatch])
  return isOpen ? (
    <ConsolePanel />
  ) : (
    <Tooltip title='Open process console'>
      <IconButton
        className={styles.openButton}
        aria-label='Open process console'
        onClick={() => dispatch(openConsole())}
      >
        <TerminalIcon />
      </IconButton>
    </Tooltip>
  )
}
