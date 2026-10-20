import { createSlice, type PayloadAction } from '@reduxjs/toolkit'
import type { State } from './types'
import type { Measurement } from '../types/presentation'
import { rootReducer } from '../store/root-store'

const initialState: State = {
  isInitialized: false
}

const slice = createSlice({
  name: 'dashboard',
  initialState,
  reducers: {
    setValue: (state, action: PayloadAction<Measurement>) =>
      deepMerge(
        state,
        Object.fromEntries(
          Object.entries(action.payload).map(([id, values]) => [id, normalizeChannelKeys(values)])
        )
      ),
    setIsInitialized: (state, action: PayloadAction<boolean>) => ({
      ...state,
      isInitialized: action.payload
    })
  },
  selectors: {
    getIsInitialized: ({ isInitialized }) => isInitialized,
    getValues: state => state
  }
})

export const store = slice.injectInto(rootReducer)

export default store

export const deepMerge = <A, B>(left: A, right: B): A & B => {
  if (right === null || right === undefined) return left as any
  if (typeof left !== 'object' || left === null || typeof right !== 'object') return right as any

  return Object.keys(left).reduce(
    (acc, key) => ({
      ...acc,
      [key]: deepMerge((left as any)[key], (right as any)[key])
    }),
    { ...right } as any
  )
}

// Only address keys are case-insensitive; presentation tags and props retain their casing.
export const normalizeChannelKeys = <T>(
  values: Record<string, Record<string, T>> | null
): Record<string, Record<string, T>> =>
  Object.fromEntries(
    Object.entries(values ?? {}).map(([periphery, channels]) => [
      periphery.toLowerCase(),
      Object.fromEntries(
        Object.entries(channels).map(([channel, value]) => [channel.toLowerCase(), value])
      )
    ])
  )

export const { setValue, setIsInitialized } = slice.actions
