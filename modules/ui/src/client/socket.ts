const webSocket = new WebSocket('/ws')

webSocket.addEventListener('open', () => {
  if (subscriber !== null) webSocket.addEventListener('message', subscriber)
  subscriber = null
  inWaitMessages.forEach((msg) => webSocket.send(msg))
  inWaitMessages = []
  console.info('WebSocket opened')
})

let inWaitMessages: string[] = []
type MessageSubscriber = (msg: MessageEvent<string>) => void
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
    webSocket.addEventListener('message', callback)
  } else {
    subscriber = callback
    console.info('Waiting for WebSocket to open. 1 subscriber waiting.')
  }
}
