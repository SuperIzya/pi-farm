import React, { useEffect } from 'react'
import type { CommandName, ProperData, ProperName } from './commands'
import type { PayloadAction } from '@reduxjs/toolkit'
import { DataNames, ExtractData } from './data'
import { PiFarmSocket } from './pi-farm-socket'
import { Receiver } from './receive'
import { useDispatch } from 'react-redux'
import type { Creator } from './types'
import { setAppConfiguration } from '../store/root-store'

export { PiFarmSocket }

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
  sendCommand: () => {},
  onReceiveData: () => {}
}

export class WebSocketContext {
  private socket: () => PiFarmSocket
  private receiver: Receiver
  constructor(socket: () => PiFarmSocket) {
    this.socket = socket
    this.receiver = new Receiver()
  }

  private ClientContext = React.createContext<ClientContextType>(contextObject)
  public useOnReceiveData = () => React.useContext(this.ClientContext).onReceiveData
  public useSendCommand = () => React.useContext(this.ClientContext).sendCommand
  public sendCommand = <T extends CommandName, D = void>(t: ProperName<T, D>, data?: ProperData<T, D>) => {}

  public Component = ({ children }: { children: React.ReactNode }) => {
    const dispatch = useDispatch()
    const [socket_, setSocket] = React.useState<PiFarmSocket | null>(null)    
    useEffect(() => {
      const s = this.socket()
      this.sendCommand = s.sendCommand.bind(s)
      setSocket(s)
      const stop = s.onMessage((event: MessageEvent<string>) => {
        if (!event.data) {
          console.warn('Received empty message from WebSocket')
          return
        }
        this.receiver.processIncoming(event.data, dispatch)
      })
      return () => {
        stop()
        s.disconnect()
        setSocket(null)
      }
    }, [])
    const Ctx = this.ClientContext
    return socket_ && (
      <Ctx.Provider
        value={{ sendCommand: socket_.sendCommand, onReceiveData: this.receiver.onReceiveData }}
      >
        {children}
      </Ctx.Provider>
    )
  }
  
  
}
export const AppWebSocket = new WebSocketContext(() => new PiFarmSocket('/appWs'))
const SetAppConfig = () => {
  const onReceiveData = AppWebSocket.useOnReceiveData()
  onReceiveData('app-configuration-data', setAppConfiguration)
  return null
}

export const AppSocket = ({ children }: { children: React.ReactNode }) => (
  <AppWebSocket.Component>
    <SetAppConfig />
    {children}
  </AppWebSocket.Component>
)
