import { controllerTypesSlice } from './store'
import { createSelector } from 'reselect'
import { getKnownTypes as getKnownPeriphery } from '../periphery-types/selectors'
import type { RootState } from './types'
import type { ItemProps } from '../../../utils/list'
import { PeripheryType } from '../../../types'

export const { getKnownTypes, getNewType } = controllerTypesSlice.selectors
const getPeripheryIndex = (state: RootState, { idx }: { idx: number }) => idx
const getTypeIndex = (state: RootState, { key }: ItemProps) => key
const getPeripheryKeys = () =>
  createSelector([getKnownTypes, getTypeIndex], (controllers, index) =>
    Object.keys(controllers[index].peripheries)
  )
const getPeriphery = () =>
  createSelector(
    [
      getPeripheryKeys(),
      getKnownPeriphery,
      getPeripheryIndex,
      getKnownTypes,
      getTypeIndex
    ],
    (keys, periphery, idx, controllers, index) => {
      const key = keys[idx]
      const id = controllers[index].peripheries[key]
      return periphery.find((p) => p.id === id) || ({} as PeripheryType)
    }
  )

export const getPeripheryImage = () =>
  createSelector([getPeriphery()], ({ image }) => image)

export const getPeripheryName = () => createSelector([getPeriphery()], ({ name }) => name)
