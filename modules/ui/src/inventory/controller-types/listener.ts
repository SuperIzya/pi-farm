import {
  startListeningCanSave,
  startListeningSave,
  TransformFunction
} from '../../store/listeners'
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
import type { ControllerType, MaybeId, NewEntity, NoId } from '../../types'

type CorrectType = Omit<MaybeId<ControllerType>, 'description'> & {
  description?: string
} & { canBeSaved: boolean }

const isNewEntityCanBeSaved = (
  newEntity: NewEntity<ControllerType> | undefined
): newEntity is CorrectType =>
  newEntity !== undefined &&
  newEntity.name !== undefined &&
  newEntity.name !== '' &&
  newEntity.code !== undefined &&
  newEntity.code !== '' &&
  newEntity.peripheries !== undefined &&
  ('schema' in newEntity
    ? newEntity.schema !== undefined && newEntity.schema !== ''
    : true) &&
  Object.keys(newEntity.peripheries).length > 0

const toNoId = (entity: Partial<ControllerType>): NoId<ControllerType> => ({
  name: entity.name || '',
  description: entity.description || '',
  code: entity.code || '',
  schema: entity.schema || '',
  peripheries: entity.peripheries || {}
})

const transformSave: TransformFunction<
  ControllerType,
  'save-controller-type',
  NoId<ControllerType>,
  'update-controller-type',
  ControllerType
> = (entity) =>
  'id' in entity
    ? {
        data: {
          ...toNoId(entity),
          id: entity.id || 0
        },
        hasId: true
      }
    : {
        hasId: false,
        data: toNoId(entity)
      }

export const createListener = () => {
  startListeningCanSave<RootState>(
    setNewEntityName,
    setNewEntityDescription,
    setNewEntitySchema,
    addNewEntityPeriphery,
    removeNewEntityPeriphery
  )(getNewEntity, isNewEntityCanBeSaved, setNewEntityCanBeSaved)

  startListeningSave<RootState>()(
    getNewEntity,
    saveNewEntity,
    setLoading,
    isNewEntityCanBeSaved,
    transformSave,
    'save-controller-type',
    'update-controller-type'
  )
}
