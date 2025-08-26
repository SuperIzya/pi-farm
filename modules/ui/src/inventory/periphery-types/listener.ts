import {
  saveNewEntity,
  setNewEntityCanBeSaved,
  setNewEntityDescription,
  setNewEntityDirection,
  setNewEntityName,
  setNewEntityImage,
  setNewEntityUnits
} from './actions'
import { NewPeripheryType, RootState } from './types'
import { getNewEntity } from './selectors'
import { isAnyOf } from '@reduxjs/toolkit'
import { rootListener } from '../../store/listeners'
import { sendCommand } from '../../client'
import { PeripheryType } from '../../types'

const isNewEntityCanBeSaved = (
  newEntity: NewPeripheryType | undefined
): newEntity is PeripheryType & { canBeSaved: boolean } =>
  newEntity !== undefined &&
  newEntity.name !== undefined &&
  newEntity.name !== '' &&
  newEntity.description !== undefined &&
  newEntity.description !== '' &&
  newEntity.direction !== undefined &&
  newEntity.image !== undefined &&
  newEntity.image !== '' &&
  newEntity.units !== undefined &&
  newEntity.units !== ''

const canBeSaved = () =>
  rootListener.startListening({
    matcher: isAnyOf(
      setNewEntityName,
      setNewEntityDescription,
      setNewEntityImage,
      setNewEntityDirection,
      setNewEntityUnits
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
        const { canBeSaved: _, ...rest } = newEntity
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
