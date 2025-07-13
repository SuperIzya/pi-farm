import React, { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './app'
import { Provider } from 'react-redux'
import { rootStore } from './store/root-store'

const div = document.getElementById('root') || document.createElement('div')

const root = createRoot(div)
root.render(
  <StrictMode>
    <Provider store={rootStore}>
      <App />
    </Provider>
  </StrictMode>
)
