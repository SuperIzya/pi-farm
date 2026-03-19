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
  setNewEntityCanBeSaved,
  saveNewEntity,
  setLoading,
  addInputToController,
  addOutputToController,
  setNewEntityNodes,
  replaceNewEntityEndpoints,
  setNewEntityEndpoints,
  removeInputFromController,
  removeOutputFromController,
  setNewEntityInputs,
  setNewEntityOutputs,
  setNewEntityPreview
} from './actions'
import type { Configuration, MaybeId, NewEntity, NoId } from '../../types'

type CorrectType = Omit<MaybeId<Configuration>, 'description'> & {
  description?: string
} & { canBeSaved: boolean }

const isNewEntityCanBeSaved = (
  newEntity: NewEntity<Configuration> | undefined
): newEntity is CorrectType =>
  newEntity !== undefined &&
  newEntity.name !== undefined &&
  newEntity.name !== '' &&
  ((newEntity.inputs !== undefined && Object.entries(newEntity.inputs).length > 0) ||
    (newEntity.outputs !== undefined && Object.entries(newEntity.outputs).length > 0)) &&
  newEntity.nodes !== undefined &&
  newEntity.nodes.length > 0 &&
  newEntity.edges !== undefined &&
  newEntity.edges.length > 0

const toNoId = (entity: Partial<Configuration>): NoId<Configuration> => ({
  name: entity.name || '',
  description: entity.description || '',
  processingUnit: entity.processingUnit || '',
  inbound: entity.inbound || [],
  outbound: entity.outbound || [],
  additional: entity.additional || {},
  inputs: entity.inputs || {},
  outputs: entity.outputs || {},
  nodes: entity.nodes || [],
  edges: entity.edges || [],
  preview: entity.preview || ''
})

const transformSave: TransformFunction<
  Configuration,
  'save-configuration',
  NoId<Configuration>,
  'update-configuration',
  Configuration
> = (entity) =>
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
    setNewEntityName,
    setNewEntityDescription,
    addInputToController,
    addOutputToController,
    setNewEntityNodes,
    replaceNewEntityEndpoints,
    setNewEntityEndpoints,
    removeInputFromController,
    removeOutputFromController,
    setNewEntityInputs,
    setNewEntityOutputs,
    setNewEntityPreview
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
