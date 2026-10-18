import { peripheryTypesSlice } from './store'

export const {
  editEntity,
  setEntities,
  startNewEntity,
  cancelNewEntity,
  setNewEntityName,
  setNewEntityDescription,
  setNewEntityImage,
  setNewEntityCanBeSaved,
  saveNewEntity,
  addNewEntity,
  setInitialized,
  setLoading,
  addNewConnection,
  editConnection,
  cancelConnection,
  setConnectionName,
  setConnectionDirection,
  setConnectionUnits,
  setConnectionType,
  setConnectionPresentation,
  setConnectionCanBeSaved,
  saveConnection,
  deleteConnection
} = peripheryTypesSlice.actions
