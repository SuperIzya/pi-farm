import {
  applyMiddleware,
  combineSlices,
  compose,
  configureStore,
  createSlice,
  PayloadAction
} from '@reduxjs/toolkit'
import { createEpicMiddleware, EpicMiddleware } from 'redux-observable'
import { rootEpic$ } from './epics'

declare global {
  interface Window {
    __REDUX_DEVTOOLS_EXTENSION_COMPOSE__: typeof compose
  }
}

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

const composeEnhancers = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose

export const rootReducer = combineSlices(loadingSlice)

const epicMiddleware: EpicMiddleware<
  PayloadAction<boolean>,
  PayloadAction<boolean>,
  typeof initialState
> = createEpicMiddleware()

export const rootStore = configureStore({
  reducer: rootReducer,
  // @ts-expect-error `gd` is not a function, but a generic function for `getDefaultMiddleware`
  middleware: (gd) =>
    gd({
      thunk: false,
      immutableCheck: false
    }).prepend(composeEnhancers(applyMiddleware(epicMiddleware))),
  devTools: process.env.NODE_ENV !== 'production'
})

export type RootState = typeof rootStore.getState

epicMiddleware.run(rootEpic$)
