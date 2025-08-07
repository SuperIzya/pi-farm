import { peripheryTypesSlice } from './store'

export const {
  editType,
  setTypes,
  startNewType,
  cancelNewType,
  setNewTypeName,
  setNewTypeDescription,
  setNewTypeImage,
  setNewTypeDirection,
  setNewTypeUnits,
  setNewTypeCanBeSaved,
  saveNewType,
  addNewType
} = peripheryTypesSlice.actions
