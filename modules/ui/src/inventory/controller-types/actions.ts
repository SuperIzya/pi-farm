import { controllerTypesSlice } from './store'

export const {
  setEntities,
  editEntity,
  cancelNewEntity,
  setNewEntityName,
  setNewEntityDescription,
  saveNewEntity,
  setNewEntityCode,
  startNewEntity,
  setNewEntitySchema,
  addNewEntityPeriphery,
  setNewEntityCanBeSaved,
  addNewEntity,
  removeNewEntityPeriphery,
  setLoading
} = controllerTypesSlice.actions
