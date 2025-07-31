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
  addNewTypePeriphery,
  setNewTypeCanBeSaved,
  addNewType,
  removeNewTypePeriphery
} = controllerTypesSlice.actions
