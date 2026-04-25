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
  setNewConnection,
  cancelNewConnection,
  saveNewConnection,
  setNewConnectionCanBeSaved,
  setNewConnectionName,
  setNewConnectionDirection,
  setNewConnectionUnits,
  setNewConnectionType
} = peripheryTypesSlice.actions
