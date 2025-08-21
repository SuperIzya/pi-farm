import type { DataNames, ExtractData, TypedData } from './data'
import type { PayloadAction } from '@reduxjs/toolkit'
import React from 'react'
import type { Creator } from './types'

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

export const processMessage = <
  T extends DataNames,
  D extends ExtractData<T> = ExtractData<T>
>(
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
