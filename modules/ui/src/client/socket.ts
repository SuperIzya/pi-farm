import type { CommandName, ProperData, ProperName } from './commands'
import { v4 as uuidv4 } from 'uuid'

const webSocket = new WebSocket('/ws')

webSocket.addEventListener('open', () => {
  if (subscriber !== null) {
    const sub = subscriber
    webSocket.addEventListener('message', sub.subscribe)
    sub.unsubscribe = () => webSocket.removeEventListener('message', sub.subscribe)
  }
  subscriber = null
  inWaitMessages.forEach(msg => webSocket.send(msg))
  inWaitMessages = []
})

let inWaitMessages: string[] = []
type MessageSubscriber = {
  subscribe: (msg: MessageEvent<string>) => void
  unsubscribe: () => void
}
let subscriber: MessageSubscriber | null = null

export const sendCommand = <T extends CommandName, D = void>(
  t: ProperName<T, D>,
  data?: ProperData<T, D>
) => sendData({ [t]: data == undefined ? {} : { data } })

const maxSize = 1024 * 32

const sendData = (data: Record<string, unknown>) => {
  if (webSocket.readyState === WebSocket.OPEN) {
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
        sendCommand('partial-command', { data, index, totalCount, id })
      )
    } else webSocket.send(str)
  } else {
    inWaitMessages = [...inWaitMessages, JSON.stringify(data)]
  }
}

export const onMessage = (callback: (msg: MessageEvent<string>) => void) => {
  if (webSocket.readyState === WebSocket.OPEN) {
    webSocket.addEventListener('message', callback)
    return () => webSocket.removeEventListener('message', callback)
  } else {
    subscriber = {
      subscribe: callback,
      unsubscribe: () => (subscriber = null)
    }
    const sub = subscriber
    return () => sub.unsubscribe()
  }
}
