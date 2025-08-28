import { DataNames, ExtractData, findTypedData, TypedData } from './data'
import type { PayloadAction } from '@reduxjs/toolkit'
import React, { Dispatch } from 'react'
import type { Creator, PartialMessage } from './types'

type Transformer<T extends DataNames, D = ExtractData<T>> = Creator<D>

type RegisteredTransformers = { [T in DataNames]?: Transformer<T> }

let dataCallbacks: RegisteredTransformers = {}

export const onReceiveData = <T extends DataNames, D = ExtractData<T>>(
  dataType: T,
  transform: Creator<D>
) => {
  dataCallbacks = {
    ...dataCallbacks,
    [dataType]: transform
  } as RegisteredTransformers
}

const getTransformer = <T extends DataNames>(dataType: T): Transformer<T> | undefined =>
  dataCallbacks[dataType]

type PartialCollector = { [id: string]: PartialMessage[] }
let partialCollector: PartialCollector = {}

export const processMessage = <
  T extends DataNames,
  D extends ExtractData<T> = ExtractData<T>
>(
  key: T,
  message: TypedData<T, D>,
  dispatch: React.Dispatch<PayloadAction<unknown>>
): void => {
  if (key === 'partial-data') {
    const msg = message[key].data as PartialMessage
    if (partialCollector[msg.id]?.find((d) => d.index === msg.index) !== undefined) return
    partialCollector = {
      ...partialCollector,
      [msg.id]: [...(partialCollector[msg.id] || []), msg]
    }
    const total = partialCollector[msg.id].length
    if (total >= msg.totalCount) {
      const seq = partialCollector[msg.id]
        .sort((d1, d2) => d1.index - d2.index)
        .reduce((acc, d) => acc + d.data, '')
      processIncoming(seq, dispatch)
    }
  } else {
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
}

export const processIncoming = (
  event: string,
  dispatch: Dispatch<PayloadAction<unknown>>
) => {
  try {
    const message = JSON.parse(event)
    const { data, key } = findTypedData(message)

    if (data !== undefined && key !== undefined) {
      processMessage(key, data, dispatch)
    } else {
      console.warn(`Received unknown message: '${JSON.stringify(message)}'`)
    }
  } catch {
    console.error(`Failed to parse message from WebSocket: '${event}'`)
  }
}
