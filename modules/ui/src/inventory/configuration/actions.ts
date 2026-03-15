import { configurationsSlice } from './store'

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
