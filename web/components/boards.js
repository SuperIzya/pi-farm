import 'rxjs';
import Client from '../utils/client';
import { connect } from 'react-redux';
import { mergeMap, map, pluck } from 'rxjs/operators'
import { ofType } from 'redux-observable';
import { reducerRegistry } from '../store/utils';
import { registerEpic } from '../store/epics';

const storeName = "Boards";
const INIT_BOARDS = "Init boards";
const InitBoardsAction = { type: INIT_BOARDS };
const SET_BOARDS_LIST = "Set boards list";
const SetBoardsListAction = list => ({ type: SET_BOARDS_LIST, list });

reducerRegistry.register(storeName, [], {
  [SET_BOARDS_LIST]: (state, { list }) => list.map(name => ({ name }))
});

registerEpic(action$ => action$
  .pipe(
    ofType(INIT_BOARDS),
    mergeMap(() => Client.get('/boards').pipe(
      pluck('data')
    )),
    map(SetBoardsListAction)
  )
);

const boardNamesSelector = state => state[storeName].map(b => b.name);

const mapStateToProps = state => ({
  boardNames: boardNamesSelector(state)
});

const mapDispatchToProps = dispatch => ({
  initBoards: () => dispatch(InitBoardsAction)
});

const connectToBoards = connect(mapStateToProps, mapDispatchToProps);

export {
  InitBoardsAction,
  boardNamesSelector,
  connectToBoards
};
