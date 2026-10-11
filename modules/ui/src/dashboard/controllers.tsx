import { createSelector } from '@reduxjs/toolkit'
import { getKnownEntities as getControllers } from '../inventory/controller/selectors'
import { controllerIdsSelector, getControllerPresentation } from './selectors'
import React from 'react'
import { GenericList, type GenericListProps } from '../utils/list-mixin'
import { useSelector } from 'react-redux'
import type { Presentation, RootState } from './types'

const countSelector = createSelector(
  controllerIdsSelector,
  ids => ids.length
)

const controllersMap = createSelector(
  getControllers,

)

const List = (p: Omit<GenericListProps<Presentation, RootState>, 'count'>) => {
  const count = useSelector(countSelector)
  const selectorFactory = getControllerPresentation
  return <GenericList<Presentation, RootState> {...p} count={count} selectorFactory={getControllerPresentation} />
}

export const Controllers = () => (
  <>
    <List />
  </>
)
