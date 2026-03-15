import { configurationsSlice } from './store'

export const { getKnownEntities, getNewEntity, getIsLoading, getIsInitialized } =
  configurationsSlice.selectors
