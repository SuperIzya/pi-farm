import { createTheme } from '@mui/material/styles'
import { type  CssVars, defaultCss } from './list-mixin'

// Extend MUI's Theme/ThemeOptions types with pi-farm specific config
declare module '@mui/material/styles' {
  interface Theme {
    piInventory: {
      peripheryList: CssVars
    }
  }
  interface ThemeOptions {
    piInventory?: {
      peripheryList?: CssVars
    }
  }
}

// Primary colour matches the existing nav-bar hardcode: rgba(44, 44, 255, 1)
export const theme = createTheme({
  piInventory: {
    peripheryList: defaultCss
  },
  palette: {
    primary: {
      main: '#2c2cff'
    }
  }
})
