import { configurationsSlice, processingUnitsSlice } from './store'

export const { getKnownEntities, getNewEntity, getIsLoading, getIsInitialized } =
  configurationsSlice.selectors

export const {
  getProcessingUnits,
  getProcessingUnitsIsLoading,
  getProcessingUnitsInitialized
} = processingUnitsSlice.selectors
