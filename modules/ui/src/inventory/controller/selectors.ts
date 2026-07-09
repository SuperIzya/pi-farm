import { useSelector } from 'react-redux'
import { controllersSlice } from './store'
import { RootState } from './types'

export const { getKnownEntities, getNewEntity, getIsLoading, getIsInitialized } =
  controllersSlice.selectors

export const useCtlSelector = useSelector.withTypes<RootState>()
