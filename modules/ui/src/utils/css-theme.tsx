import React from 'react'
import { ThemeProvider, alpha } from '@mui/material/styles'
import GlobalStyles from '@mui/material/GlobalStyles'
import { theme } from './theme'
import type { Theme } from '@mui/material/styles'

// Maps MUI theme tokens → CSS custom properties consumed by SCSS.
// Layout stays in SCSS; this file is the single source of truth for design tokens.
const cssVars = (t: Theme) => ({
  ':root': {
    // Primary palette
    '--color-primary': t.palette.primary.main,
    '--color-primary-contrast': t.palette.primary.contrastText,
    '--color-primary-a12': alpha(t.palette.primary.main, 0.12),
    '--color-primary-a16': alpha(t.palette.primary.main, 0.16),
    '--color-primary-a23': alpha(t.palette.primary.main, 0.23),
    '--color-primary-a24': alpha(t.palette.primary.main, 0.24),
    // Text
    '--text-primary': t.palette.text.primary,
    '--text-secondary': t.palette.text.secondary,
    // Surface
    '--background-color': t.palette.background.paper,
    '--hover-background-color': t.palette.action.hover,
    '--border-color': t.palette.divider
  } as React.CSSProperties
})

export const CssThemeProvider = ({ children }: { children: React.ReactNode }) => (
  <ThemeProvider theme={theme}>
    <GlobalStyles styles={cssVars} />
    {children}
  </ThemeProvider>
)
