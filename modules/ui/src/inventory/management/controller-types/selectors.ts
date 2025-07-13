import { controllerTypesSlice } from './store'
import { createSelector } from 'reselect'
import { getKnownTypes as getKnownPeriphery } from '../periphery-types/selectors'
import type { RootState } from './types'
import type { ItemArg } from '../../../utils/list'
import { PeripheryType } from '../periphery-types/types'

export const { getKnownTypes, getNewType } = controllerTypesSlice.selectors
const getPeripheryIndex = (state: RootState, { idx }: { idx: number }) => idx
const getTypeIndex = (state: RootState, { index }: ItemArg) => index
const getPeriphery = () =>
  createSelector(
    [getKnownTypes, getKnownPeriphery, getTypeIndex, getPeripheryIndex],
    (controllers, periphery, index, idx) => {
      const id = controllers[index].periphery[idx]
      return periphery.find((p) => p.id === id) || ({} as PeripheryType)
    }
  )

export const getPeripheryPicture = () =>
  createSelector([getPeriphery()], (periphery) => periphery.picture)

export const getPeripheryName = () =>
  createSelector([getPeriphery()], (periphery) => periphery.name)
