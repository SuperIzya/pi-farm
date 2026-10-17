import { type DataNames, type ExtractData, type TypedData, findTypedData } from './data'
import type { PayloadAction } from '@reduxjs/toolkit'
import { Dispatch } from 'react'
import type { Creator, PartialMessage } from './types'

type Transformer<T extends DataNames, D = ExtractData<T>, P = D> = Creator<D, P>

type RegisteredTransformers = { [T in DataNames]?: Transformer<T> }

type PartialCollector = { [id: string]: PartialMessage[] }

export class Receiver {

  private dataCallbacks: RegisteredTransformers = {}

public onReceiveData = <T extends DataNames, D = ExtractData<T>, P = D>(
  dataType: T,
  transform: Creator<D, P>
) => {
  this.dataCallbacks = {
    ...this.dataCallbacks,
    [dataType]: transform
  } as RegisteredTransformers
}

  private getTransformer = <T extends DataNames>(dataType: T): Transformer<T> | undefined =>
    this.dataCallbacks[dataType]
  private partialCollector: PartialCollector = {}

  public processMessage = <T extends DataNames, D extends ExtractData<T> = ExtractData<T>>(
  key: T,
  message: TypedData<T, D>,
  dispatch: Dispatch<PayloadAction<unknown>>
): void => {
  if (key === 'partial-data') {
    const msg = message[key].data as PartialMessage
    if (this.partialCollector[msg.id]?.find(d => d.index === msg.index) !== undefined) return
    this.partialCollector = {
      ...this.partialCollector,
      [msg.id]: [...(this.partialCollector[msg.id] || []), msg]
    }
    const total = this.partialCollector[msg.id].length
    if (total >= msg.totalCount) {
      const seq = this.partialCollector[msg.id]
        .sort((d1, d2) => d1.index - d2.index)
        .reduce((acc, d) => acc + d.data, '')
      this.partialCollector = {}
      this.processIncoming(seq, dispatch)
    }
  } else {
    const callback = this.getTransformer(key)
    if (callback !== undefined) {
      const data = message[key].data as ExtractData<T>
      if (data !== undefined) {
        dispatch(callback(data))
      } else {
        console.warn(
          `Undefined data for key '${key}' in message: '${JSON.stringify(message, null, 2)}'`
        )
      }
    } else {
      console.warn(`No callback registered for data type: ${JSON.stringify(message, null, 2)}`)
    }
  }
}

  public processIncoming = (event: string, dispatch: Dispatch<PayloadAction<unknown>>) => {
  try {
    const message = JSON.parse(event)
    const { data, key } = findTypedData(message)

    if (data !== undefined && key !== undefined) {
      this.processMessage(key, data, dispatch)
    } else {
      console.warn(`Received unknown message: '${JSON.stringify(message)}'`)
    }
  } catch {
    console.error(`Failed to parse message from WebSocket: '${event}'`)
  }
}
}
