import React, { useEffect } from 'react'
import type { CommandName, ProperData, ProperName } from './commands'
import type { PayloadAction } from '@reduxjs/toolkit'
import { Data, DataType, GenericData } from './data'
import { useDispatch } from 'react-redux'

const webSocket = new WebSocket('/ws')

const sendData = (data: Record<string, unknown>) => webSocket.send(JSON.stringify(data))

export const sendCommand = <T extends CommandName, D = void>(
  t: ProperName<T, D>,
  data?: ProperData<T, D>
) => sendData({ [t]: data == undefined ? {} : { data } })

type NoType<T> = Extract<Data & { type: T }, 'data'>

type Transformer<T extends DataType, D = NoType<T>> = (
  data: GenericData<T, D>
) => PayloadAction<D>

type RegisteredTransformers = { [T in DataType]?: Transformer<T> }

let dataCallbacks: RegisteredTransformers = {}

type CommandsContextType = {
  sendCommand: <T extends CommandName, D = void>(
    t: ProperName<T, D>,
    data?: ProperData<T, D>
  ) => void
  onReceiveData: <T extends DataType, D = NoType<T>>(
    dataType: T,
    callback: Transformer<T, D>
  ) => void
}

const onReceiveData = <T extends DataType, D = NoType<T>>(
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
const getTransformer = <T extends DataType, D = NoType<T>>(
  dataType: T
): Transformer<T, D> | undefined =>
  dataCallbacks[dataType] as Transformer<T, D> | undefined

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
        const callback = getTransformer(data.type)

        if (callback !== undefined) {
          const action = callback(message.data)
          dispatch(action)
        } else {
          console.warn(`No callback registered for data type: ${message.type}`)
        }
      } else {
        console.warn(`Received unknown message: '${JSON.stringify(message, null, 2)}'`)
      }
    } catch {
      console.error(`Failed to parse message from WebSocket: '${event.data}'`, event)
    }
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
