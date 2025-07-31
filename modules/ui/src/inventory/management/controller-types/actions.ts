import { controllerTypesSlice } from './store'

export const {
  setTypes,
  editType,
  cancelNewType,
  setNewTypeName,
  setNewTypeDescription,
  saveNewType,
  setNewTypeCode,
  startNewType,
  setNewTypeSchema,
  setNewTypePeriphery,
  setNewTypeCanBeSaved,
  addNewType,
  removeNewTypePeriphery
} = controllerTypesSlice.actions
