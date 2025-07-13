import { BehaviorSubject, mergeMap } from 'rxjs'
import { combineEpics, Epic } from 'redux-observable'
import { PayloadAction } from '@reduxjs/toolkit'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type AnyAction = PayloadAction<any>

const epic$ = new BehaviorSubject(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  combineEpics<AnyAction, AnyAction, any, any>() // ts-ignore: no implicit any
)

export const addEpic = <In = void, Out extends In = In, S = void, D = void>(
  epic: Epic<PayloadAction<In>, PayloadAction<Out>, S, D>
) => epic$.next(epic)

export const rootEpic$: Epic<AnyAction, AnyAction, object> = (action$, state$) =>
  epic$.pipe(mergeMap((epic) => epic(action$, state$, {})))
