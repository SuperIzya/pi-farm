import {
  ActionCreatorWithoutPayload,
  createListenerMiddleware,
  ActionCreatorWithPayload,
  isAnyOf,
  ActionCreatorWithOptionalPayload
} from '@reduxjs/toolkit'
import { NewEntity } from '../types'
import { sendCommand } from '../client'
import type { CommandName, ProperData, ProperName } from '../client/commands'

export const rootListener = createListenerMiddleware()

type ValidateFunction<Entity> = (
  newEntity: NewEntity<Entity> | undefined
) => newEntity is NewEntity<Entity>

export type TransformFunction<
  Entity,
  SaveName extends CommandName,
  SavePayload,
  UpdateName extends CommandName,
  NewEntityType = Entity
> = (newEntity: NewEntity<NewEntityType>) =>
  | {
      data: ProperData<SaveName, SavePayload>
      hasId: false
    }
  | {
      data: ProperData<UpdateName, Entity>
      hasId: true
    }

export const startListeningSave =
  <State>() =>
  <
    Entity,
    NewEntityType,
    SaveCmd extends CommandName,
    UpdateCmd extends CommandName,
    SavePayload,
    Sel extends (state: State) => NewEntity<NewEntityType> | undefined,
    S extends string = string
  >(
    selector: Sel,
    saveAction: ActionCreatorWithoutPayload<S>,
    setLoading: ActionCreatorWithPayload<boolean>,
    validate: ValidateFunction<NewEntityType>,
    transform: TransformFunction<Entity, SaveCmd, SavePayload, UpdateCmd, NewEntityType>,
    saveCommandName: ProperName<SaveCmd, SavePayload>,
    updateCommandName: ProperName<UpdateCmd, Entity>
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



export const startListeningSaveMemo =
  <State>() =>
  <
    Entity,
    NewEntityType,
    SaveCmd extends CommandName,
    UpdateCmd extends CommandName,
    SavePayload,
    S extends string = string
  >(
    selector: (state: State) => ReturnType<TransformFunction<Entity, SaveCmd, SavePayload, UpdateCmd, NewEntityType>> | false,
    saveAction: ActionCreatorWithoutPayload<S>,
    setLoading: ActionCreatorWithPayload<boolean>,
    saveCommandName: ProperName<SaveCmd, SavePayload>,
    updateCommandName: ProperName<UpdateCmd, Entity>
  ) =>
    rootListener.startListening({
      type: saveAction.type,
      effect: (_, listenerApi) => {
        const newEntity = selector(listenerApi.getState() as State)

        if (newEntity !== false) {
          listenerApi.dispatch(setLoading(true))
          const { data, hasId } = newEntity

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
    setCanSave: ActionCreatorWithPayload<boolean>,
  ) => 
    rootListener.startListening({
      matcher: isAnyOf(...matchers),
      effect: (_, listenerApi) => {
        const newEntity = selector(listenerApi.getState() as State)
        const canBeSaved = validate(newEntity)
        if(canBeSaved !== newEntity?.canBeSaved) {
          listenerApi.dispatch(setCanSave(canBeSaved))
        }
      }
    })


export const startListeningCanSaveMemo =
  <State, T>(...matchers: ActionCreatorWithOptionalPayload<any>[]) =>
  (
    selector: (state: State) => T | false,
    newEntitySelector: (state: State) => NewEntity<unknown> | undefined,
    setCanSave: ActionCreatorWithPayload<boolean>,
  ) => 
    rootListener.startListening({
      matcher: isAnyOf(...matchers),
      effect: (_, listenerApi) => {
        const state = listenerApi.getState() as State
        const canBeSaved = selector(state) !== false
        const newEntity = newEntitySelector(state)
        if(canBeSaved !== newEntity?.canBeSaved) {
          listenerApi.dispatch(setCanSave(canBeSaved))
        }
      }
    })
  