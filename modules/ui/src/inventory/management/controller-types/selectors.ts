import { controllerTypesSlice } from './store'
import { createSelector } from 'reselect'
import { getKnownEntities as getKnownPeriphery } from '../periphery-types/selectors'
import type { RootState } from './types'
import { getListKey } from '../../../utils/list-mixin'
import { PeripheryType } from '../../../types'

export const { getKnownEntities, getNewEntity, getIsLoading } =
  controllerTypesSlice.selectors

export const getControllerTypesKeys = createSelector([getKnownEntities], (entities) =>
  Object.typedKeys(entities)
)

export const sortPeripheriesKeys = (keys: string[]) =>
  keys.sort((a, b) => (a > b ? 1 : -1))

const getControllerIndex = (state: RootState, { idx }: { idx: number }) => idx
const getPeripheryKeys = () =>
  createSelector([getKnownEntities, getControllerIndex], (controllers, index) =>
    sortPeripheriesKeys(Object.keys(controllers[index].peripheries))
  )
const getPeriphery = () =>
  createSelector(
    [
      getPeripheryKeys(),
      getControllerIndex,
      getKnownPeriphery,
      getKnownEntities,
      getListKey
    ],
    (keys, idx, periphery, controllers, itemKey) => {
      const key = keys[itemKey]
      const id = controllers[idx].peripheries[key]
      return periphery[id] || ({} as PeripheryType)
    }
  )

export const getPeripheryKey = () =>
  createSelector([getPeripheryKeys(), getListKey], (keys, index) => keys[index])

export const getPeripheryImage = () =>
  createSelector([getPeriphery()], ({ image }) => image)

export const getPeripheryName = () => createSelector([getPeriphery()], ({ name }) => name)
