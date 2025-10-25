import { controllersSlice } from './store'

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
  setNewEntityTypeId,
  setNewEntityName,
  setInitialized
} = controllersSlice.actions
