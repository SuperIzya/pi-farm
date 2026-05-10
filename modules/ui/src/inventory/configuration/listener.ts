import { startListeningCanSave, startListeningSave, TransformFunction } from '../../store/listeners'
import type { ConfigurationGraph, RootState } from './types'
import { getNewEntity } from './selectors'
import {
  setNewEntityCanBeSaved,
  saveNewEntity,
  setLoading,
  setName,
  setDescription
} from './actions'
import type { MaybeId, NewEntity, New, Configuration, ConfigurationId, DataConnection } from '../../types'

type CorrectType = Omit<MaybeId<ConfigurationGraph, ConfigurationId>, 'description'> & {
  description?: string
} & { canBeSaved: boolean }

const isNewEntityCanBeSaved = (
  newEntity: NewEntity<ConfigurationGraph> | undefined
): newEntity is CorrectType =>
  newEntity !== undefined
  && newEntity.name !== undefined
  && newEntity.name !== ''


const toNoId = (entity: Partial<ConfigurationGraph>): New<Configuration> => ({
  name: entity.name || '',
  description: entity.description || '',
  processingUnits: Object.keys(entity.processingUnits ?? {}),
  connections: entity.edges?.map(({data}) => data).filter(c => c !== undefined) || []
})

const transformSave: TransformFunction<
  Configuration,
  'save-configuration',
  New<Configuration>,
  'update-configuration',
  ConfigurationGraph
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
  startListeningCanSave<RootState>(
    setName,
    setDescription
  )(getNewEntity, isNewEntityCanBeSaved, setNewEntityCanBeSaved)

  startListeningSave<RootState>()(
    getNewEntity,
    saveNewEntity,
    setLoading,
    isNewEntityCanBeSaved,
    transformSave,
    'save-configuration',
    'update-configuration'
  )
}
