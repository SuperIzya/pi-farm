import { rootListener } from '../../../store/listeners'
import type { RootState } from './types'
import { getNewType } from './selectors'
import {
  setNewTypeDescription,
  setNewTypeName,
  addNewTypePeriphery,
  setNewTypeSchema,
  setNewTypeCanBeSaved,
  removeNewTypePeriphery
} from './actions'
import { isAnyOf } from '@reduxjs/toolkit'
import type { NewType } from '../types'
import type { ControllerType } from '../../../types'

const isNewTypeCanBeSaved = (
  newType: NewType<ControllerType> | undefined
): newType is ControllerType & { canBeSaved: boolean } =>
  newType !== undefined &&
  newType.name !== undefined &&
  newType.name !== '' &&
  newType.description !== undefined &&
  newType.description !== '' &&
  newType.peripheries !== undefined &&
  Object.keys(newType.peripheries).length > 0

export const createListener = () =>
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
