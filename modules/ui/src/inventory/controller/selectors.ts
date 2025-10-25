import { controllersSlice } from './store'

export const { getKnownEntities, getNewEntity, getIsLoading, getIsInitialized } =
  controllersSlice.selectors
