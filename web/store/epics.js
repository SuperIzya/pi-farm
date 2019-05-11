import {BehaviorSubject} from 'rxjs';
import { mergeMap, mapTo, filter, switchMap } from 'rxjs/operators';
import {combineEpics} from 'redux-observable';

const empty = action$ => action$.pipe(mapTo(null), filter(Boolean));
const epic$ = new BehaviorSubject(empty);
const registerEpic = epic => epic$.next(combineEpics(epic, epic$.value));

const rootEpic = (action$, store) => epic$.pipe(
    switchMap(epic => epic(action$, store))
  );

export {
  rootEpic,
  epic$,
  registerEpic
}
