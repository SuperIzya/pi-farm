import { peripheryTypesSlice } from './store'

export const {
  editEntity,
  setEntities,
  startNewEntity,
  cancelNewEntity,
  setNewEntityName,
  setNewEntityDescription,
  setNewEntityImage,
  setNewEntityDirection,
  setNewEntityUnits,
  setNewEntityCanBeSaved,
  saveNewEntity,
  addNewEntity,
  setInitialized,
  setLoading
} = peripheryTypesSlice.actions
