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
  inWaitMessages.forEach((msg) => webSocket.send(msg))
  inWaitMessages = []
  console.info('WebSocket opened')
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
    if (str.length > maxSize && Object.keys(data).indexOf('partial-command') === -1) {
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
    console.info(
      `Waiting for WebSocket to open. ${inWaitMessages.length} messages waiting.`
    )
  }
}

export const onMessage = (callback: (msg: MessageEvent<string>) => void) => {
  if (webSocket.readyState === WebSocket.OPEN) {
    console.info('Added listener to WebSocket.')
    webSocket.addEventListener('message', callback)
    return () => {
      webSocket.removeEventListener('message', callback)
      console.info('Removed listener from WebSocket.')
    }
  } else {
    subscriber = {
      subscribe: callback,
      unsubscribe: () => {
        subscriber = null
      }
    }
    const sub = subscriber
    console.info('Waiting for WebSocket to open. 1 subscriber waiting.')
    return () => {
      console.info('Removed subscriber from WebSocket.')
      sub.unsubscribe()
    }
  }
}
