import { rootListener, startListeningCanSave } from '../../store/listeners'
import { clearError, setError } from '../../store/root-store'
import { AppWebSocket } from '../../client'
import type { RootState, NewControllerType } from './types'
import { getNewEntity } from './selectors'
import {
  setNewEntityDescription,
  setNewEntityName,
  setNewEntityCodeArchive,
  addNewEntityPeriphery,
  setNewEntitySchema,
  setNewEntityCanBeSaved,
  removeNewEntityPeriphery,
  editEntity,
  saveNewEntity,
  setLoading
} from './actions'
import type { ControllerType, New } from '../../types'

const isNewEntityCanBeSaved = (
  entity: NewControllerType | undefined
): entity is NewControllerType =>
  entity !== undefined
  && !!entity.name
  && (entity.id !== undefined || !!entity.codeArchive)
  && entity.peripheries !== undefined
  && Object.keys(entity.peripheries).length > 0

const toNoId = (entity: NewControllerType): New<ControllerType> => ({
  name: entity.name || '',
  description: entity.description || '',
  schema: entity.schema || '',
  peripheries: entity.peripheries || {},
  presentation: entity.presentation ?? null
})

export const createListener = () => {
  startListeningCanSave<RootState>(
    setNewEntityName,
    setNewEntityDescription,
    setNewEntitySchema,
    setNewEntityCodeArchive,
    addNewEntityPeriphery,
    removeNewEntityPeriphery,
    editEntity
  )(getNewEntity, isNewEntityCanBeSaved, setNewEntityCanBeSaved)

  rootListener.startListening({
    actionCreator: saveNewEntity,
    effect: (_, api) => {
      const entity = getNewEntity(api.getState() as RootState)
      if (!isNewEntityCanBeSaved(entity)) {
        api.dispatch(setLoading(false))
        api.dispatch(
          setError('A name, periphery and project archive are required for a new controller type.')
        )
        return
      }

      api.dispatch(setLoading(true))
      try {
        const data = toNoId(entity)
        if (entity.id !== undefined) {
          AppWebSocket.sendCommand(
            'update-controller-type',
            { ...data, id: entity.id },
            entity.codeArchive ? { codeArchive: entity.codeArchive } : undefined
          )
        } else if (entity.codeArchive) {
          AppWebSocket.sendCommand('save-controller-type', data, {
            codeArchive: entity.codeArchive
          })
        }
      } catch (error) {
        api.dispatch(setLoading(false))
        api.dispatch(
          setError(error instanceof Error ? error.message : 'Failed to save controller type.')
        )
      }
    }
  })

  rootListener.startListening({
    actionCreator: clearError,
    effect: (_, api) => {
      api.dispatch(setLoading(false))
    }
  })
}
