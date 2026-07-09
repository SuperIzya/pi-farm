import { useSelector } from 'react-redux'
import { controllerTypesSlice } from './store'
import { RootState } from './types'

export const { getKnownEntities, getNewEntity, getIsLoading, getIsInitialized } =
  controllerTypesSlice.selectors

export const sortPeripheriesKeys = <T>(keys: T[]) => keys.sort((a, b) => (a > b ? 1 : -1))
export const useCTSelector = useSelector.withTypes<RootState>()
