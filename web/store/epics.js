import {BehaviorSubject} from 'rxjs';
import {mergeMap, mapTo, filter} from 'rxjs/operators';
import {combineEpics} from 'redux-observable';

const empty = action$ => action$.pipe(mapTo(null), filter(Boolean));
const epic$ = new BehaviorSubject(combineEpics(
  empty,
  empty
));
const registerEpic = epic => epic$.next(epic);

const rootEpic = (action$, store) => epic$.pipe(
    mergeMap(epic => epic(action$, store))
  );

export {
  rootEpic,
  epic$,
  registerEpic
}
