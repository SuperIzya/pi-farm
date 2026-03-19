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
  setNewEntityDescription,
  setNewEntityName,
  setInitialized,
  setNewEntityInputs,
  setNewEntityOutputs,
  addInputToController,
  addOutputToController,
  removeInputFromController,
  removeOutputFromController,
  setNewEntityEndpoints,
  replaceNewEntityEndpoints,
  setNewEntityNodes,
  setNewEntityPreview
} = configurationsSlice.actions

export const {
  setProcessingUnitsInitialized,
  setProcessingUnitsIsLoading,
  setProcessingUnits
} = processingUnitsSlice.actions
