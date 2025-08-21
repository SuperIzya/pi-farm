import React, { useEffect } from 'react'
import type { CommandName, ProperData, ProperName } from './commands'
import type { PayloadAction } from '@reduxjs/toolkit'
import { DataNames, dataNames, ExtractData, findDataType } from './data'
import { sendData, onMessage } from './socket'
import { useDispatch } from 'react-redux'
import { onReceiveData, processMessage } from './receive'
import type { Creator } from './types'

export const sendCommand = <T extends CommandName, D = void>(
  t: ProperName<T, D>,
  data?: ProperData<T, D>
) => sendData({ [t]: data == undefined ? {} : { data } })

type ClientContextType = {
  sendCommand: <T extends CommandName, D = void>(
    t: ProperName<T, D>,
    data?: ProperData<T, D>
  ) => void
  onReceiveData: <T extends DataNames, D extends ExtractData<T> = ExtractData<T>>(
    dataType: T,
    callback: Creator<D>
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

    try {
      const message = JSON.parse(event.data)
      const obj = dataNames.map(findDataType(message)).find((p) => p !== undefined)

      if (obj !== undefined) {
        const key = dataNames.find((n) => n in message)
        if (key !== undefined) {
          processMessage(key, obj, dispatch)
        }
      } else {
        console.warn(`Received unknown message: '${JSON.stringify(message)}'`)
      }
    } catch {
      console.error(`Failed to parse message from WebSocket: '${event.data}'`, event)
    }
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
