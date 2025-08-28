import { rootListener } from '../../store/listeners'
import type { RootState } from './types'
import { getNewEntity } from './selectors'
import {
  setNewEntityDescription,
  setNewEntityName,
  addNewEntityPeriphery,
  setNewEntitySchema,
  setNewEntityCanBeSaved,
  removeNewEntityPeriphery,
  saveNewEntity,
  setLoading
} from './actions'
import { isAnyOf } from '@reduxjs/toolkit'
import type { ControllerType, MaybeId, NewEntity } from '../../types'
import { sendCommand } from '../../client'

type CorrectType = Omit<MaybeId<ControllerType>, 'description'> & {
  description?: string
} & { canBeSaved: boolean }

const isNewEntityCanBeSaved = (
  newEntity: NewEntity<ControllerType> | undefined
): newEntity is CorrectType =>
  newEntity !== undefined &&
  newEntity.name !== undefined &&
  newEntity.name !== '' &&
  newEntity.peripheries !== undefined &&
  ('schema' in newEntity
    ? newEntity.schema !== undefined && newEntity.schema !== ''
    : true) &&
  Object.keys(newEntity.peripheries).length > 0

const canBeSaved = () =>
  rootListener.startListening({
    matcher: isAnyOf(
      setNewEntityName,
      setNewEntityDescription,
      setNewEntitySchema,
      addNewEntityPeriphery,
      removeNewEntityPeriphery
    ),
    effect: (_, listenerApi) => {
      const newEntity = getNewEntity(listenerApi.getState() as RootState)

      const canBeSaved = isNewEntityCanBeSaved(newEntity)

      listenerApi.dispatch(setNewEntityCanBeSaved(canBeSaved))
    }
  })

const save = () =>
  rootListener.startListening({
    type: saveNewEntity.type,
    effect: (_, listenerApi) => {
      const newEntity = getNewEntity(listenerApi.getState() as RootState)

      if (isNewEntityCanBeSaved(newEntity)) {
        listenerApi.dispatch(setLoading(true))
        const { canBeSaved: _, description, ...rest } = newEntity
        if ('id' in rest)
          sendCommand('update-controller-type', {
            ...rest,
            id: rest.id || 0,
            description: description || ''
          })
        else
          sendCommand('save-controller-type', {
            ...rest,
            description: description || ''
          })
      }
    }
  })

export const createListener = () => {
  canBeSaved()
  save()
}
