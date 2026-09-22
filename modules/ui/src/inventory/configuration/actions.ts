import { configurationsSlice, processingUnitsSlice } from './store'

export const {
  setEntities,
  setNewEntityCanBeSaved,
  saveNewEntity,
  setSvgPreview,
  addNewEntity,
  startNewEntity,
  cancelNewEntity,
  editEntity,
  setEditGraph,
  setLoading,
  setInitialized,
  removeEdge,
  selectEdge,
  removeControllerNode,
  addControllerNode,
  addProcessorNode,
  removeProcessorNode,
  setProcessorParams,
  resetGraph,
  addEdge,
  setPreviewSvg,
  setName,
  setDescription
} = configurationsSlice.actions

export const {
  setProcessingUnitsInitialized,
  setProcessingUnitsIsLoading,
  setProcessingUnits,
  addProcessingUnit
} = processingUnitsSlice.actions
