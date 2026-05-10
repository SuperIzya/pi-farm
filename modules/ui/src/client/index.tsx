import React, { useEffect } from 'react'
import type { CommandName, ProperData, ProperName } from './commands'
import type { PayloadAction } from '@reduxjs/toolkit'
import { DataNames, ExtractData } from './data'
import { sendCommand, onMessage } from './socket'
import { useDispatch } from 'react-redux'
import { onReceiveData, processIncoming } from './receive'
import type { Creator } from './types'

export { sendCommand } from './socket'

export type ClientContextType = {
  sendCommand: <T extends CommandName, D = void>(
    t: ProperName<T, D>,
    data?: ProperData<T, D>
  ) => void
  onReceiveData: <T extends DataNames, D extends ExtractData<T> = ExtractData<T>, P = D>(
    dataType: T,
    callback: Creator<D, P>
  ) => void
}

const ClientContext = React.createContext<ClientContextType>({
  sendCommand,
  onReceiveData
})

const startListening = (dispatch: React.Dispatch<PayloadAction<unknown>>) =>
  onMessage((event: MessageEvent<string>) => {
    if (!event.data) {
      console.warn('Received empty message from WebSocket')
      return
    }
    processIncoming(event.data, dispatch)
  })

export const useOnReceiveData = () => React.useContext(ClientContext).onReceiveData

export const useSendCommand = () => React.useContext(ClientContext).sendCommand

export const CommandsDispatcher = ({ children }: { children: React.ReactNode }) => {
  const dispatch = useDispatch()
  const [initialized, setInitialized] = React.useState(false)
  useEffect(() => {
    if (!initialized) {
      startListening(dispatch)
      setInitialized(true)
    }
  }, [])
  return (
    <ClientContext.Provider value={{ sendCommand, onReceiveData }}>
      {children}
    </ClientContext.Provider>
  )
}
