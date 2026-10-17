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
    setValue: (state, action: PayloadAction<Measurement>) => deepMerge(state, action.payload),
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
  if(right === null || right === undefined) return left as any
  if (typeof left !== 'object' || left === null || typeof right !== 'object') return right as any
  
  return Object.keys(left).reduce(
    (acc, key) => ({
      ...acc,
      [key]: key in left ? deepMerge((left as any)[key], (right as any)[key]) : (right as any)[key] as any
    }),
    right as any
  )
}


export const { setValue, setIsInitialized } = slice.actions