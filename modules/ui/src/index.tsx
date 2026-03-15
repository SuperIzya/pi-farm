import React, { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './app'
import { Provider } from 'react-redux'
import { rootStore } from './store/root-store'

const div = document.getElementById('root') || document.createElement('div')

declare global {
  // eslint-disable-next-line @typescript-eslint/consistent-type-definitions
  interface ObjectConstructor {
    typedKeys<T>(obj: T): Array<keyof T>
  }
}
// eslint-disable-next-line @typescript-eslint/no-explicit-any
Object.typedKeys = Object.keys as any

const root = createRoot(div)
root.render(
  <StrictMode>
    <Provider store={rootStore}>
      <App />
    </Provider>
  </StrictMode>
)
