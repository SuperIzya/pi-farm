import {
  saveNewType,
  setNewTypeCanBeSaved,
  setNewTypeDescription,
  setNewTypeDirection,
  setNewTypeName,
  setNewTypeImage,
  setNewTypeUnits
} from './actions'
import type { RootState } from './types'
import { getNewType } from './selectors'
import { isAnyOf } from '@reduxjs/toolkit'
import { rootListener } from '../../../store/listeners'
import { sendCommand } from '../../../client'
import { PeripheryType } from '../../../types'
import { NewType } from '../types'

const isNewTypeCanBeSaved = (
  newType: NewType<PeripheryType> | undefined
): newType is PeripheryType & { canBeSaved: boolean } =>
  newType !== undefined &&
  newType.name !== undefined &&
  newType.name !== '' &&
  newType.description !== undefined &&
  newType.description !== '' &&
  newType.direction !== undefined &&
  newType.image !== undefined &&
  newType.image !== '' &&
  newType.units !== undefined &&
  newType.units !== ''

const canBeSaved = () =>
  rootListener.startListening({
    matcher: isAnyOf(
      setNewTypeName,
      setNewTypeDescription,
      setNewTypeImage,
      setNewTypeDirection,
      setNewTypeUnits
    ),
    effect: (_, listenerApi) => {
      const newType = getNewType(listenerApi.getState() as RootState)

      const canBeSaved = isNewTypeCanBeSaved(newType)

      listenerApi.dispatch(setNewTypeCanBeSaved(canBeSaved))
    }
  })

const save = () =>
  rootListener.startListening({
    type: saveNewType.type,
    effect: (_, listenerApi) => {
      const newType = getNewType(listenerApi.getState() as RootState)

      if (isNewTypeCanBeSaved(newType)) {
        const { canBeSaved: _, ...rest } = newType
        if ('id' in rest) sendCommand('update-periphery-type', rest)
        else sendCommand('save-periphery-type', rest)
      } else {
        console.error('New type is not valid, cannot save')
        return
      }
    }
  })

export const createListener = () => {
  canBeSaved()
  save()
}
