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

export const sendData = (data: Record<string, unknown>) => {
  if (webSocket.readyState === WebSocket.OPEN) {
    webSocket.send(JSON.stringify(data))
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
