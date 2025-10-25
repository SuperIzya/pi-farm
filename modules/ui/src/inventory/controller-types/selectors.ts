import { controllerTypesSlice } from './store'

export const { getKnownEntities, getNewEntity, getIsLoading, getIsInitialized } =
  controllerTypesSlice.selectors

export const sortPeripheriesKeys = <T>(keys: T[]) => keys.sort((a, b) => (a > b ? 1 : -1))
