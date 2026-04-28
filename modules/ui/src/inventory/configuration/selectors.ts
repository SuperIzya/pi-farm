import { configurationsSlice, graphSlice, processingUnitsSlice } from './store'

export const { getKnownEntities, getNewEntity, getIsLoading, getIsInitialized } =
  configurationsSlice.selectors

export const { getProcessingUnits, getProcessingUnitsIsLoading, getProcessingUnitsInitialized } =
  processingUnitsSlice.selectors

export const { getSelectedControllerId, getSelectedPeripheryId } = graphSlice.selectors
