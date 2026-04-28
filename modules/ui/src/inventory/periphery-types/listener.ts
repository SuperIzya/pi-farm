import {
  saveNewEntity,
  setNewEntityCanBeSaved,
  setNewEntityDescription,
  setNewEntityName,
  setNewEntityImage,
  setLoading,
  saveConnection,
  setConnectionType,
  setConnectionDirection,
  setConnectionUnits,
  setConnectionName,
  setConnectionCanBeSaved
} from './actions'
import { NewPeripheryType, RootState } from './types'
import { getCurrentConnection, getNewEntity } from './selectors'
import { startListeningCanSave, startListeningSave, TransformFunction } from '../../store/listeners'
import { New, NewEntity, PeripheryConnection, PeripheryType } from '../../types'

const isNewEntityCanBeSaved = (
  newEntity: NewPeripheryType | undefined
): newEntity is PeripheryType & { canBeSaved: boolean } =>
  newEntity !== undefined
  && newEntity.name !== undefined
  && newEntity.name !== ''
  && newEntity.description !== undefined
  && newEntity.description !== ''
  && newEntity.image !== undefined
  && newEntity.image !== ''
  && newEntity.connections !== undefined
  && Object.keys(newEntity.connections).length > 0

const isNewConnectionCanBeSaved = (
  connection: NewEntity<PeripheryConnection> | undefined
): connection is PeripheryConnection & { canBeSaved: boolean } =>
  connection !== undefined
  && connection.name !== undefined
  && connection.name !== ''
  && connection.direction !== undefined
  && connection.units !== undefined
  && connection.units !== ''
  && connection.type !== undefined
  && connection.type !== ''

const toNoId = (entity: Partial<PeripheryType>): New<PeripheryType> => ({
  name: entity.name || '',
  description: entity.description || '',
  image: entity.image || '',
  connections: entity.connections || []
})

const transformSave: TransformFunction<
  PeripheryType,
  'save-periphery-type',
  New<PeripheryType>,
  'update-periphery-type',
  PeripheryType
> = entity =>
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
    saveConnection
  )(getNewEntity, isNewEntityCanBeSaved, setNewEntityCanBeSaved)

  startListeningCanSave<RootState>(
    setConnectionName,
    setConnectionDirection,
    setConnectionUnits,
    setConnectionType
  )(getCurrentConnection, isNewConnectionCanBeSaved, setConnectionCanBeSaved)

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
