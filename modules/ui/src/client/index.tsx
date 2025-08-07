import React, { useEffect } from 'react'
import type { Command } from './commands'
import type { PayloadAction } from '@reduxjs/toolkit'
import { Data, DataType, GenericData } from './data'
import { useDispatch } from 'react-redux'

const webSocket = new WebSocket('/ws')

export const sendCommand = (command: Command) =>
  webSocket.send(
    JSON.stringify({
      command
    })
  )

type Transformer<T extends DataType, D = Omit<Data, 'type'>> = (
  data: GenericData<T, D>
) => PayloadAction<Omit<D, 'type'>>

type RegisteredTransformers = { [T in DataType]?: Transformer<T, Data & { type: T }> }

let dataCallbacks: RegisteredTransformers = {}

type CommandsContextType = {
  sendCommand: (command: Command) => void
  onReceiveData: <T extends DataType, D = Omit<Data & { type: T }, 'type'>>(
    dataType: T,
    callback: Transformer<T, D>
  ) => void
}

const onReceiveData = <T extends DataType, D = Omit<Data & { type: T }, 'type'>>(
  dataType: T,
  transform: Transformer<T, D>
) => {
  dataCallbacks = {
    ...dataCallbacks,
    [dataType]: transform
  } as RegisteredTransformers
}

const CommandsContext = React.createContext<CommandsContextType>({
  sendCommand,
  onReceiveData
})

const isData = (o: object): o is Data => 'type' in o

const startListening = (dispatch: React.Dispatch<PayloadAction<unknown>>) =>
  (webSocket.onmessage = (event: MessageEvent<string>) => {
    if (!event.data) {
      console.warn('Received empty message from WebSocket')
      return
    }

    try {
      const message = JSON.parse(event.data)

      if (isData(message)) {
        const data: Data = message as Data
        const callback = dataCallbacks[data.type] as Transformer<typeof message.type>

        if (callback !== undefined) {
          const action = callback(message)
          dispatch(action)
        } else {
          console.warn(`No callback registered for data type: ${message.type}`)
        }
      }
    } catch {}
  })

export const useOnReceiveData = () => React.useContext(CommandsContext).onReceiveData

export const useSendCommand = () => React.useContext(CommandsContext).sendCommand

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
    <CommandsContext.Provider value={{ sendCommand, onReceiveData }}>
      {children}
    </CommandsContext.Provider>
  )
}
