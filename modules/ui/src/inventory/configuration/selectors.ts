import { configurationsSlice, processingUnitsSlice } from './store'

export const {
  getKnownEntities,
  getNewEntity,
  getIsLoading,
  getIsInitialized,
  getControllers,
  getProcessingUnits: getConfigurationProcessingUnits
} = configurationsSlice.selectors

export const {
  getProcessingUnits: getAllProcessingUnits,
  getProcessingUnitsIsLoading,
  getProcessingUnitsInitialized
} = processingUnitsSlice.selectors
