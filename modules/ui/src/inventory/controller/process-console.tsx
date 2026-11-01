import React, { useEffect, useRef, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { IconButton, Tooltip } from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import TerminalIcon from '@mui/icons-material/Terminal'
import { AppWebSocket } from '../../client'
import {
  closeConsole,
  openConsole,
  addProcessLog,
  setProcessState
} from './actions'
import type { RootState } from './types'
import * as styles from './process-console.scss'
import type { ProcessLog } from '../../types'
import { 
  getIsOpen,
  getLogs,
  getProcess } from './selectors'

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

const MIN_HEIGHT = 80

const ResizeHandle = ({ panel, onResize }: {
  panel: React.RefObject<HTMLElement | null>
  onResize: (height: number) => void
}) => {
  const start = useRef<{ y: number; height: number } | null>(null)
  return (
    <div
      className={styles.resizeHandle}
      role='separator'
      aria-orientation='horizontal'
      aria-label='Resize console'
      onPointerDown={(e) => {
        e.currentTarget.setPointerCapture(e.pointerId)
        start.current = { y: e.clientY, height: panel.current?.offsetHeight ?? 0 }
      }}
      onPointerMove={(e) => {
        if (!start.current) return
        const max = window.innerHeight - MIN_HEIGHT
        onResize(Math.min(max, Math.max(MIN_HEIGHT, start.current.height + start.current.y - e.clientY)))
      }}
      onPointerUp={() => {
        start.current = null
      }}
    />
  )
}

const ConsolePanel = () => {
  const dispatch = useDispatch()
  const panel = useRef<HTMLElement>(null)
  const [height, setHeight] = useState<number>()
  return (
    <section
      ref={panel}
      className={styles.panel}
      aria-label='Process console'
      style={height === undefined ? undefined : ({ '--console-height': `${height}px` } as React.CSSProperties)}
    >
      <ResizeHandle panel={panel} onResize={setHeight} />
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
  onReceive('process-logs', addProcessLog)
  onReceive('process-state', setProcessState)
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
