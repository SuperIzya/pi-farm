import { controllerTypesSlice } from './store'

export const {
  setEntities,
  editEntity,
  cancelNewEntity,
  setNewEntityName,
  setNewEntityDescription,
  saveNewEntity,
  setNewEntityCodeArchive,
  setUploading,
  startNewEntity,
  setNewEntitySchema,
  addNewEntityPeriphery,
  setNewEntityCanBeSaved,
  addNewEntity,
  removeNewEntityPeriphery,
  setLoading,
  setInitialized
} = controllerTypesSlice.actions
