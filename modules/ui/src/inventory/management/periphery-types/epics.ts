import { map, Observable, withLatestFrom } from 'rxjs'
import { ofType, StateObservable } from 'redux-observable'
import {
  setNewTypeCanBeSaved,
  setNewTypeDescription,
  setNewTypeDirection,
  setNewTypeName,
  setNewTypePicture,
  setNewTypeUnits
} from './actions'
import { type AnyAction } from '../../../store/epics'
import type { RootState } from './types'
import { getNewType } from './selectors'

export const newTypeCanBeSaved = (
  action$: Observable<AnyAction>,
  state$: StateObservable<RootState>
) =>
  action$.pipe(
    ofType(
      setNewTypeName.type,
      setNewTypeDescription.type,
      setNewTypePicture.type,
      setNewTypeDirection.type,
      setNewTypeUnits.type
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
        newType.direction !== undefined &&
        newType.units !== undefined &&
        newType.units !== ''

      return setNewTypeCanBeSaved(canBeSaved)
    })
  )
