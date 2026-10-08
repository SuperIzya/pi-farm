import { createSlice, type PayloadAction } from '@reduxjs/toolkit'
import type { State } from './types'

const initialState: State = {
  isInitialized: false
}

const store = createSlice({
  name: 'dashboard',
  initialState,
  reducers: {
    setValue: (state, action: PayloadAction<State>) => deepMerge(state, action.payload),
    setIsInitialized: (state, action: PayloadAction<boolean>) => ({
      ...state,
      isInitialized: action.payload
    })
  },
  selectors: {
    getIsInitialized: ({ isInitialized }) => isInitialized,
    getValues: state => {
      const { isInitialized: _, ...values } = state
      return values
    }
  }
})

export default store

const deepMerge = <T>(left: T, right: T): T => {
  if (typeof left !== 'object' || left === null) return right
  if (typeof right !== 'object' || right === null) return left
  return Object.keys(right).reduce(
    (acc, key) => ({
      ...acc,
      [key]: key in left ? deepMerge((left as any)[key], (right as any)[key]) : (right as any)[key]
    }),
    left
  )
}
