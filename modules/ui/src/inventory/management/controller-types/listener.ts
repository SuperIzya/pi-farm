import { rootListener } from '../../../store/listeners'
import type { RootState } from './types'
import { getNewType } from './selectors'
import {
  setNewTypeDescription,
  setNewTypeName,
  addNewTypePeriphery,
  setNewTypeSchema,
  setNewTypeCanBeSaved,
  removeNewTypePeriphery,
  saveNewType
} from './actions'
import { isAnyOf } from '@reduxjs/toolkit'
import type { NewType } from '../types'
import type { ControllerType } from '../../../types'
import { sendCommand } from '../../../client'

const isNewTypeCanBeSaved = (
  newType: NewType<ControllerType> | undefined
): newType is ControllerType & { canBeSaved: boolean } =>
  newType !== undefined &&
  newType.name !== undefined &&
  newType.name !== '' &&
  newType.description !== undefined &&
  newType.description !== '' &&
  newType.peripheries !== undefined &&
  ('schema' in newType ? newType.schema !== undefined && newType.schema !== '' : true) &&
  Object.keys(newType.peripheries).length > 0

const canBeSaved = () =>
  rootListener.startListening({
    matcher: isAnyOf(
      setNewTypeName,
      setNewTypeDescription,
      setNewTypeSchema,
      addNewTypePeriphery,
      removeNewTypePeriphery
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
        if ('id' in rest) sendCommand('update-controller-type', rest)
        else sendCommand('save-controller-type', rest)
      }
    }
  })

export const createListener = () => {
  canBeSaved()
  save()
}
