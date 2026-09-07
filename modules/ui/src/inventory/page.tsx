import React from 'react'
import { RootState } from '../store/root-store'
import { AddButton } from './form-mixin'
import type { ClassName } from '../types'
import { Button, IconButton } from '@mui/material'
import FileUploadIcon from '@mui/icons-material/FileUpload'
import DownloadIcon from '@mui/icons-material/Download'
import { WaitLoading } from '../utils/wait-loading'

type Props<State extends RootState> = {
  styles: Record<string, string>
  children: React.ReactNode
  getIsLoading: (state: State) => boolean
  title: string
  addEntityText: string
}

export const InventoryPage = <S extends RootState>({
  styles,
  children,
  getIsLoading,
  title,
  addEntityText
}: Props<S>) => (
  <div className={styles.container}>
    <h1 className={styles.header}>{title}</h1>
    <div className={styles.buttons}>
      <ImportData className={styles.import} />
      <AddButton className={styles.add} text={addEntityText} />
    </div>

    <WaitLoading isLoadingSelector={getIsLoading}>{children}</WaitLoading>
  </div>
)

export const ImportData = ({ className }: ClassName) => {
  const onClick = () => {
    // TODO: hook up actual import logic
  }
  return (
    <Button
      variant='outlined'
      startIcon={<FileUploadIcon />}
      onClick={onClick}
      className={className}
    >
      Import
    </Button>
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
