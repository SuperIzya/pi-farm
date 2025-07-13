import { peripheryTypesSlice } from './store'

export const {
  editType,
  setTypes,
  startNewType,
  cancelNewType,
  setNewTypeName,
  setNewTypeDescription,
  setNewTypePicture,
  setNewTypeDirection,
  setNewTypeUnits,
  setNewTypeCanBeSaved,
  saveNewType,
  addNewType
} = peripheryTypesSlice.actions
