import type { CommandName, ProperData, ProperName } from './commands'
import { v4 as uuidv4 } from 'uuid'

type MessageSubscriber = {
  subscribe: (msg: MessageEvent<string>) => void
  unsubscribe: () => void
}
const maxSize = 1024 * 32
export class PiFarmSocket {
  private webSocket: WebSocket
  private inWaitMessages: string[] = []
  private subscriber: MessageSubscriber | null = null

  constructor(url: string) {
    this.webSocket = new WebSocket(url)

    this.webSocket.addEventListener('open', this.onOpen)
  }

  private onOpen = () => {
    if (this.subscriber !== null) {
      const sub = this.subscriber
      this.webSocket.addEventListener('message', sub.subscribe)
      sub.unsubscribe = () => this.webSocket.removeEventListener('message', sub.subscribe)
    }
    this.inWaitMessages.forEach(msg => this.webSocket.send(msg))
    this.inWaitMessages = []
    this.webSocket.removeEventListener('open', this.onOpen)
  }

  sendCommand = <T extends CommandName, D = void>(t: ProperName<T, D>, data?: ProperData<T, D>) =>
    this.sendData({ [t]: data == undefined ? {} : { data } })

  private sendData = (data: Record<string, unknown>) => {
    if (this.webSocket.readyState === WebSocket.OPEN) {
      const str = JSON.stringify(data)
      if (str.length > maxSize && !('partial-command' in data)) {
        const collect = (left: string, collected: string[]): string[] => {
          if (left.length === 0) return collected
          if (left.length <= maxSize) return [...collected, left]
          const head = left.slice(0, maxSize)
          const tail = left.slice(maxSize)
          return collect(tail, [...collected, head])
        }
        const id = uuidv4()
        collect(str, []).forEach((data, index, { length: totalCount }) =>
          this.sendCommand('partial-command', { data, index, totalCount, id })
        )
      } else this.webSocket.send(str)
    } else {
      this.inWaitMessages = [...this.inWaitMessages, JSON.stringify(data)]
    }
  }

  onMessage = (callback: (msg: MessageEvent<string>) => void) => {
    if (this.webSocket.readyState === WebSocket.OPEN) {
      this.webSocket.addEventListener('message', callback)
      return () => this.webSocket.removeEventListener('message', callback)
    } else {
      this.subscriber = {
        subscribe: callback,
        unsubscribe: () => (this.subscriber = null)
      }
      const sub = this.subscriber
      return () => sub.unsubscribe()
    }
  }
}
