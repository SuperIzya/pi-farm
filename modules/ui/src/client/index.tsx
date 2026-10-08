import React, { useEffect } from 'react'
import type { CommandName, ProperData, ProperName } from './commands'
import type { PayloadAction } from '@reduxjs/toolkit'
import { DataNames, ExtractData } from './data'
import { PiFarmSocket } from './pi-farm-socket'
import { useDispatch } from 'react-redux'
import { onReceiveData, processIncoming } from './receive'
import type { Creator } from './types'
import { setAppConfiguration } from '../store/root-store'

export { PiFarmSocket }

const appSocket = new PiFarmSocket('/appWs')

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

const contextObject: ClientContextType = {
  sendCommand: appSocket.sendCommand,
  onReceiveData: onReceiveData
}

const ClientContext = React.createContext<ClientContextType>(contextObject)

export const { sendCommand } = contextObject

const startListening = (socket: PiFarmSocket, dispatch: React.Dispatch<PayloadAction<unknown>>) =>
  socket.onMessage((event: MessageEvent<string>) => {
    if (!event.data) {
      console.warn('Received empty message from WebSocket')
      return
    }
    processIncoming(event.data, dispatch)
  })

export const useOnReceiveData = () => React.useContext(ClientContext).onReceiveData

export const useSendCommand = () => React.useContext(ClientContext).sendCommand

export const WebSocketContext =
  (socket: PiFarmSocket) =>
  ({ children }: { children: React.ReactNode }) => {
    const dispatch = useDispatch()
    const [initialized, setInitialized] = React.useState(false)
    useEffect(() => {
      if (!initialized) {
        startListening(socket, dispatch)
        setInitialized(true)
      }
    }, [])
    return (
      <ClientContext.Provider
        value={{ sendCommand: socket.sendCommand, onReceiveData: onReceiveData }}
      >
        {children}
      </ClientContext.Provider>
    )
  }

export const CommandsDispatcher = ({ children }: { children: React.ReactNode }) => {
  const AppWebSocketContext = WebSocketContext(appSocket)
  const [initialized, setInitialized] = React.useState(false)
  useEffect(() => {
    if (!initialized) {
      onReceiveData('app-configuration-data', setAppConfiguration)
      setInitialized(true)
    }
  }, [])
  return <AppWebSocketContext>{children}</AppWebSocketContext>
}
