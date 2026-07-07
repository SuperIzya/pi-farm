import React, { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './app'
import { Provider } from 'react-redux'
import { rootStore } from './store/root-store'
import { CssThemeProvider } from './utils/css-theme'

const div = document.getElementById('root') || document.createElement('div')

declare global {
  interface ObjectConstructor {
    typedKeys<T>(obj: T): Array<keyof T>
  }
}

Object.typedKeys = Object.keys as any

const root = createRoot(div)
root.render(
  <StrictMode>
    <Provider store={rootStore}>
      <CssThemeProvider>
        <App />
      </CssThemeProvider>
    </Provider>
  </StrictMode>
)
