import React, { useEffect } from 'react'
import type { CommandName, ProperData, ProperName } from './commands'
import type { PayloadAction } from '@reduxjs/toolkit'
import { DataNames, dataNames, ExtractData, TypedData } from './data'
import { useDispatch } from 'react-redux'

const webSocket = new WebSocket('/ws')

const sendData = (data: Record<string, unknown>) => webSocket.send(JSON.stringify(data))

export const sendCommand = <T extends CommandName, D = void>(
  t: ProperName<T, D>,
  data?: ProperData<T, D>
) => sendData({ [t]: data == undefined ? {} : { data } })

type Creator<D> = (arg: D) => PayloadAction<D>
type Transformer<T extends DataNames, D = ExtractData<T>> = Creator<D>

type RegisteredTransformers = { [T in DataNames]?: Transformer<T> }

let dataCallbacks: RegisteredTransformers = {}

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

const onReceiveData = <T extends DataNames, D = ExtractData<T>>(
  dataType: T,
  transform: Creator<D>
) => {
  dataCallbacks = {
    ...dataCallbacks,
    [dataType]: transform
  } as RegisteredTransformers
}

const ClientContext = React.createContext<ClientContextType>({
  sendCommand,
  onReceiveData
})

const isDataTyped = <T extends DataNames>(
  name: T,
  obj: object
): obj is TypedData<T, ExtractData<T>> => name in obj

const findDataType =
  <T extends DataNames>(obj: object) =>
  (name: T): TypedData<T, ExtractData<T>> | undefined => {
    if (isDataTyped(name, obj)) {
      return obj
    }
  }

const getTransformer = <T extends DataNames>(dataType: T): Transformer<T> | undefined =>
  dataCallbacks[dataType]

const processMessage = <T extends DataNames, D extends ExtractData<T> = ExtractData<T>>(
  key: T,
  message: TypedData<T, D>,
  dispatch: React.Dispatch<PayloadAction<unknown>>
): void => {
  const callback = getTransformer(key)
  if (callback !== undefined) {
    const data = message[key].data as ExtractData<T>
    if (data !== undefined) {
      dispatch(callback(data))
    } else {
      console.warn(`Undefined data in message: ${JSON.stringify(message)}`)
    }
  } else {
    console.warn(`No callback registered for data type: ${JSON.stringify(message)}`)
  }
}

const startListening = (dispatch: React.Dispatch<PayloadAction<unknown>>) =>
  (webSocket.onmessage = (event: MessageEvent<string>) => {
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
