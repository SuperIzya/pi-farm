import { rootListener } from '../../store/listeners'
import type { RootState } from './types'
import { getNewEntity } from './selectors'
import {
  setNewEntityDescription,
  setNewEntityName,
  setNewEntityCanBeSaved,
  saveNewEntity,
  setLoading,
  setNewEntityTypeId
} from './actions'
import { isAnyOf } from '@reduxjs/toolkit'
import type { Controller, MaybeId, NewEntity } from '../../types'
import { sendCommand } from '../../client'

type CorrectType = Omit<MaybeId<Controller>, 'description'> & {
  description?: string
} & { canBeSaved: boolean }

const isNewEntityCanBeSaved = (
  newEntity: NewEntity<Controller> | undefined
): newEntity is CorrectType =>
  newEntity !== undefined &&
  newEntity.name !== undefined &&
  newEntity.name !== '' &&
  newEntity.typeId !== undefined

const canBeSaved = () =>
  rootListener.startListening({
    matcher: isAnyOf(setNewEntityName, setNewEntityDescription, setNewEntityTypeId),
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
          sendCommand('update-controller', {
            ...rest,
            id: rest.id || 0,
            description: description || ''
          })
        else
          sendCommand('save-controller', {
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
