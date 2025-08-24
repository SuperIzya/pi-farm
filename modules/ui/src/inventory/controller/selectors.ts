import { controllersSlice } from './store'
import { createSelector } from 'reselect'

export const { getKnownEntities, getNewEntity, getIsLoading } = controllersSlice.selectors

export const getControllerKeys = createSelector(getKnownEntities, (entities) =>
  Object.keys(entities)
)
