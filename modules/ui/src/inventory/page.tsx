import React from 'react'
import type { RootState } from '../store/root-store'
import { AddButton } from '../utils/form-mixin'
import type { ClassName } from '../types'
import { Button, IconButton } from '@mui/material'
import FileUploadIcon from '@mui/icons-material/FileUpload'
import DownloadIcon from '@mui/icons-material/Download'
import { WaitLoading } from '../utils/wait-loading'
import { useSendCommand } from '../client'
import type { CommandName, ProperName } from '../client/commands'

type Props<State extends RootState> = {
  styles: Record<string, string>
  children: React.ReactNode
  getIsLoading: (state: State) => boolean
  getDataCommands: ProperName<CommandName, void>[]
  title: string
  addEntityText: string
}

export const InventoryPage = <S extends RootState>({
  styles,
  children,
  getIsLoading,
  getDataCommands,
  title,
  addEntityText
}: Props<S>) => (
  <div className={styles.container}>
    <h1 className={styles.header}>{title}</h1>
    <div className={styles.buttons}>
      <ImportData className={styles.import} getDataCommands={getDataCommands} />
      <AddButton className={styles.add} text={addEntityText} />
    </div>

    <WaitLoading isLoadingSelector={getIsLoading}>{children}</WaitLoading>
  </div>
)

export const ImportData = ({
  className,
  getDataCommands
}: ClassName & { getDataCommands: ProperName<CommandName, void>[] }) => {
  const sendCommand = useSendCommand()

  const onSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault()

    void fetch('/api/import', {
      method: 'POST',
      body: new FormData(event.currentTarget)
    })
      .then(() => getDataCommands.forEach(n => sendCommand(n)))
      .catch(console.error)
  }

  return (
    <form encType='multipart/form-data' onSubmit={onSubmit}>
      <Button
        component='label'
        variant='outlined'
        startIcon={<FileUploadIcon />}
        className={className}
      >
        Import
        <input
          hidden
          type='file'
          name='file'
          onChange={({ currentTarget }) => {
            if (currentTarget.files?.length) {
              currentTarget.form?.requestSubmit()
            }
          }}
        />
      </Button>
    </form>
  )
}

type ExportCommand = 'periphery-type' | 'controller-type' | 'controller' | 'configuration'

type ExportDataProps<C extends ExportCommand> = ClassName & {
  id: number
  command: C
}

export const ExportData = <C extends ExportCommand>({
  className,
  id,
  command
}: ExportDataProps<C>) => {
  const onClick = () =>
    fetch(`/api/export/${command}/${id}`)
      .then(r =>
        r.blob().then(blob => {
          const url = window.URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = `${command}-${id}.tar.gz`
          a.click()
          window.URL.revokeObjectURL(url)
        })
      )
      .catch(console.error)
  return (
    <IconButton onClick={onClick} className={className}>
      <DownloadIcon />
    </IconButton>
  )
}
