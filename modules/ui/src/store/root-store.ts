import {
  combineSlices,
  configureStore,
  createSlice,
  PayloadAction
} from '@reduxjs/toolkit'
import { rootListener } from './listeners'

const initialState = {
  isLoading: false
}

const loadingSlice = createSlice({
  name: 'loading',
  initialState,
  reducers: {
    setLoading: (state, action: PayloadAction<boolean>) => ({
      ...state,
      isLoading: action.payload
    })
  }
})

export const rootReducer = combineSlices(loadingSlice)

export const rootStore = configureStore({
  reducer: rootReducer,
  // @ts-expect-error `gd` is not a function, but a generic function for `getDefaultMiddleware`
  middleware: () => [rootListener.middleware],
  devTools: process.env.NODE_ENV !== 'production'
})

export const { setLoading } = loadingSlice.actions

export type RootState = typeof rootStore.getState
