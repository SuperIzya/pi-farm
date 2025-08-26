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
  setLoading
} = peripheryTypesSlice.actions
