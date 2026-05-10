import { configurationsSlice, processingUnitsSlice } from './store'

export const {
  setEntities,
  setNewEntityCanBeSaved,
  saveNewEntity,
  addNewEntity,
  startNewEntity,
  cancelNewEntity,
  editEntity,
  setLoading,
  setInitialized,
  removeConnection,
  removeControllerNode,
  addControllerNode,
  addProcessorNode,
  removeProcessorNode,
  resetGraph,
  addEdge,
  setName,
  setDescription
} = configurationsSlice.actions

export const {
  setProcessingUnitsInitialized,
  setProcessingUnitsIsLoading,
  setProcessingUnits,
  addProcessingUnit
} = processingUnitsSlice.actions
