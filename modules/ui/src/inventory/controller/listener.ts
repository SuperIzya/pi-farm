import { startListeningCanSave, startListeningSave, TransformFunction } from '../../store/listeners'
import type { RootState } from './types'
import { getNewEntity } from './selectors'
import {
  setNewEntityDescription,
  setNewEntityName,
  saveNewEntity,
  setLoading,
  setNewEntityTypeId,
  setNewEntityCanBeSaved
} from './actions'
import type { Controller, MaybeId, NewEntity, New, ControllerId } from '../../types'

type CorrectType = Omit<MaybeId<Controller, ControllerId>, 'description'> & {
  description?: string
} & { canBeSaved: boolean }

const isNewEntityCanBeSaved = (
  newEntity: NewEntity<Controller> | undefined
): newEntity is CorrectType =>
  newEntity !== undefined
  && newEntity.name !== undefined
  && newEntity.name !== ''
  && newEntity.typeId !== undefined

const toNoId = (entity: Partial<Controller>): New<Controller> => ({
  name: entity.name || '',
  description: entity.description || '',
  typeId: entity.typeId || 0
})

const transformSave: TransformFunction<
  Controller,
  'save-controller',
  New<Controller>,
  'update-controller',
  Controller
> = entity =>
  'id' in entity
    ? {
        hasId: true,
        data: {
          ...toNoId(entity),
          id: entity.id || 0
        }
      }
    : {
        hasId: false,
        data: toNoId(entity)
      }

export const createListener = () => {
  startListeningCanSave<RootState>(setNewEntityName, setNewEntityDescription, setNewEntityTypeId)(
    getNewEntity,
    isNewEntityCanBeSaved,
    setNewEntityCanBeSaved
  )

  startListeningSave<RootState>()(
    getNewEntity,
    saveNewEntity,
    setLoading,
    isNewEntityCanBeSaved,
    transformSave,
    'save-controller',
    'update-controller'
  )
}
