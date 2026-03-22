import {
  saveNewEntity,
  setNewEntityCanBeSaved,
  setNewEntityDescription,
  setNewEntityDirection,
  setNewEntityName,
  setNewEntityImage,
  setNewEntityUnits,
  setLoading
} from './actions'
import { NewPeripheryType, RootState } from './types'
import { getNewEntity } from './selectors'
import {
  startListeningCanSave,
  startListeningSave,
  TransformFunction
} from '../../store/listeners'
import { New, PeripheryType } from '../../types'

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

const toNoId = (entity: Partial<PeripheryType>): New<PeripheryType> => ({
  name: entity.name || '',
  description: entity.description || '',
  direction: entity.direction || 'both',
  image: entity.image || '',
  type: entity.type || '',
  units: entity.units || ''
})

const transformSave: TransformFunction<
  PeripheryType,
  'save-periphery-type',
  New<PeripheryType>,
  'update-periphery-type',
  PeripheryType
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
        data: toNoId(entity),
        hasId: false
      }

export const createListener = () => {
  startListeningCanSave<RootState>(
    setNewEntityName,
    setNewEntityDescription,
    setNewEntityImage,
    setNewEntityDirection,
    setNewEntityUnits
  )(getNewEntity, isNewEntityCanBeSaved, setNewEntityCanBeSaved)

  startListeningSave<RootState>()(
    getNewEntity,
    saveNewEntity,
    setLoading,
    isNewEntityCanBeSaved,
    transformSave,
    'save-periphery-type',
    'update-periphery-type'
  )
}
