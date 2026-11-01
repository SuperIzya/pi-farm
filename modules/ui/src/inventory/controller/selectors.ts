import { useSelector } from 'react-redux'
import { controllersSlice, processConsoleSlice } from './store'
import { RootState } from './types'

export const { getKnownEntities, getNewEntity, getIsLoading, getIsInitialized } =
  controllersSlice.selectors

export const { getIsOpen, getLogs, getProcess } = processConsoleSlice.selectors

export const useCtlSelector = useSelector.withTypes<RootState>()
