import { combineSlices, configureStore, createSlice, PayloadAction } from '@reduxjs/toolkit'
import { rootListener } from './listeners'
import type { AppConfiguration } from './types'

type BaseState = {
  error?: string
  appConfiguration?: AppConfiguration
}

const initialState: BaseState = {}

const errorSlice = createSlice({
  name: 'root',
  initialState,
  reducers: {
    setError: (state, action: PayloadAction<string>) => ({
      ...state,
      error: action.payload
    }),
    clearError: ({ error: _, ...state }) => state,
    setAppConfiguration: (state, action: PayloadAction<AppConfiguration>) => ({
      ...state,
      appConfiguration: action.payload
    })
  },
  selectors: {
    getError: ({ error }) => error,
    getAppConfiguration: ({ appConfiguration }) => appConfiguration
  }
})

export const rootReducer = combineSlices(errorSlice)

export const rootStore = configureStore({
  reducer: rootReducer,
  // @ts-expect-error `gd` is not a function, but a generic function for `getDefaultMiddleware`
  middleware: () => [rootListener.middleware],
  devTools: process.env.NODE_ENV !== 'production'
})

export const { setError, clearError, setAppConfiguration } = errorSlice.actions
export const { getError, getAppConfiguration } = errorSlice.selectors

export type RootState = ReturnType<typeof rootStore.getState>
