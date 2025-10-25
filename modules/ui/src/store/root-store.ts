import {
  combineSlices,
  configureStore,
  createSlice,
  PayloadAction
} from '@reduxjs/toolkit'
import { rootListener } from './listeners'

type BaseState = {
  error?: string
}

const initialState: BaseState = {}

const loadingSlice = createSlice({
  name: 'root',
  initialState,
  reducers: {
    setError: (state, action: PayloadAction<string>) => ({
      ...state,
      error: action.payload
    }),
    clearError: (state) => ({
      ...state,
      error: undefined
    })
  },
  selectors: {
    getError: ({ error }) => error
  }
})

export const rootReducer = combineSlices(loadingSlice)

export const rootStore = configureStore({
  reducer: rootReducer,
  // @ts-expect-error `gd` is not a function, but a generic function for `getDefaultMiddleware`
  middleware: () => [rootListener.middleware],
  devTools: process.env.NODE_ENV !== 'production'
})

export const { setError, clearError } = loadingSlice.actions
export const { getError } = loadingSlice.selectors

export type RootState = typeof rootStore.getState
