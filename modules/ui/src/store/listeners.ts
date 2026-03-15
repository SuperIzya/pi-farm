import {
  ActionCreatorWithoutPayload,
  createListenerMiddleware,
  ActionCreatorWithPayload,
  ActionMatchingAnyOf,
  isAnyOf,
  ActionCreator,
  ActionCreatorWithOptionalPayload
} from '@reduxjs/toolkit'
import { NewEntity } from '../types'
import { sendCommand } from '../client'
import type { CommandName, ProperData, ProperName } from '../client/commands'

export const rootListener = createListenerMiddleware()

type ValidateFunction<Entity> = (
  newEntity: NewEntity<Entity> | undefined
) => newEntity is NewEntity<Entity> & { canBeSaved: boolean }

export type TransformFunction<
  Entity,
  SaveName extends CommandName,
  SavePayload,
  UpdateName extends CommandName,
  UpdatePayload
> = (newEntity: NewEntity<Entity> & { canBeSaved: boolean }) =>
  | { data: ProperData<SaveName, SavePayload>; hasId: false }
  | {
      data: ProperData<UpdateName, UpdatePayload>
      hasId: true
    }

export const startListeningSave =
  <State>() =>
  <
    Entity,
    SaveCmd extends CommandName,
    UpdateCmd extends CommandName,
    SavePayload,
    UpdatePayload,
    Sel extends (state: State) => NewEntity<Entity> | undefined,
    S extends string = string
  >(
    selector: Sel,
    saveAction: ActionCreatorWithoutPayload<S>,
    setLoading: ActionCreatorWithPayload<boolean>,
    validate: ValidateFunction<Entity>,
    transform: TransformFunction<Entity, SaveCmd, SavePayload, UpdateCmd, UpdatePayload>,
    saveCommandName: ProperName<SaveCmd, SavePayload>,
    updateCommandName: ProperName<UpdateCmd, UpdatePayload>
  ) =>
    rootListener.startListening({
      type: saveAction.type,
      effect: (_, listenerApi) => {
        const newEntity = selector(listenerApi.getState() as State)

        if (validate(newEntity)) {
          listenerApi.dispatch(setLoading(true))
          const { data, hasId } = transform(newEntity)

          if (hasId) {
            sendCommand(updateCommandName, data)
          } else {
            sendCommand(saveCommandName, data)
          }
        }
      }
    })

export const startListeningCanSave =
  <State>(...matchers: ActionCreatorWithOptionalPayload<any>[]) =>
  <Entity>(
    selector: (state: State) => NewEntity<Entity> | undefined,
    validate: ValidateFunction<Entity>,
    setCanSave: ActionCreatorWithPayload<boolean>
  ) =>
    rootListener.startListening({
      matcher: isAnyOf(...matchers),
      effect: (_, listenerApi) => {
        const newEntity = selector(listenerApi.getState() as State)
        const canBeSaved = validate(newEntity)
        listenerApi.dispatch(setCanSave(canBeSaved))
      }
    })
