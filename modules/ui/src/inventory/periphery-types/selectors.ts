import { peripheryTypesSlice } from './store'
import { RootState } from './types'

export const { getKnownEntities, getNewEntity, getIsLoading, getIsInitialized } =
  peripheryTypesSlice.selectors

export const getCurrentConnection = (state: RootState) =>  state.periphery?.newConnection