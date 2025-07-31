import { map, Observable, withLatestFrom } from 'rxjs'
import { ofType, StateObservable } from 'redux-observable'
import { type AnyAction } from '../../../store/epics'
import type { RootState } from './types'
import { getNewType } from './selectors'
import {
  setNewTypeDescription,
  setNewTypeName,
  addNewTypePeriphery,
  setNewTypeSchema,
  setNewTypeCanBeSaved,
  removeNewTypePeriphery
} from './actions'

export const newTypeCanBeSaved = (
  action$: Observable<AnyAction>,
  state$: StateObservable<RootState>
) =>
  action$.pipe(
    ofType(
      setNewTypeName.type,
      setNewTypeDescription.type,
      setNewTypeSchema.type,
      addNewTypePeriphery.type,
      removeNewTypePeriphery.type
    ),
    withLatestFrom(state$),
    map(([, state]) => {
      const newType = getNewType(state)

      const canBeSaved =
        newType !== undefined &&
        newType.name !== undefined &&
        newType.name !== '' &&
        newType.description !== undefined &&
        newType.description !== '' &&
        newType.peripheries !== undefined &&
        Object.keys(newType.peripheries).length > 0

      return setNewTypeCanBeSaved(canBeSaved)
    })
  )
