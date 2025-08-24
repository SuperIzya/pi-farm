import { peripheryTypesSlice } from './store'
import { createSelector } from 'reselect'

export const { getKnownEntities, getNewEntity, getIsLoading } =
  peripheryTypesSlice.selectors

export const getKnownPeripheryKeys = createSelector([getKnownEntities], (entities) =>
  Object.typedKeys(entities)
)
