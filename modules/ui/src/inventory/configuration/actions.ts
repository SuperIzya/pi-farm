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
  removeEdge,
  selectEdge,
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
