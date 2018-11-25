import { mergeMap, map, pluck, filter, single, concatAll, take, toArray } from 'rxjs/operators'
import { createSelector } from 'reselect';
import { connect } from 'react-redux';
import { ofType } from 'redux-observable';
import Client from '../utils/client';
import { reducerRegistry } from '../store/utils';
import { registerEpic } from '../store/epics';
import socket from '../utils/socket';

const storeName = "Boards";
const INIT_BOARDS = "Init boards";
const InitBoardsAction = { type: INIT_BOARDS };
const SET_BOARDS_LIST = "Set boards list";
const SetBoardsListAction = list => ({ type: SET_BOARDS_LIST, list });
const LOG_FROM_BOARD = "Log from board";
const LogFromBoardAction = log => ({ type: LOG_FROM_BOARD, log });

const initalState = {
  logs: [],
  boards: [],
  showLogs: 10
};

const circularAdd = (array, elem, max) => single(elem).pipe(
  concatAll(array),
  take(max),
  toArray()
);

const reducer = {
  [SET_BOARDS_LIST]: (state, { list }) => ({ ...initalState, boards: list.map(name => ({ name })) }),
  [LOG_FROM_BOARD]: (state, { log }) => ({ ...state, logs: circularAdd(state.logs, log, state.showLogs) })
};

const initEpic = action$ => action$
.pipe(
  ofType(INIT_BOARDS),
  mergeMap(() => Client.get('/boards').pipe(
    pluck('data')
  )),
  map(SetBoardsListAction)
);

const logEpic = action$ => action$
.pipe(
  ofType(SET_BOARDS_LIST),
  mergeMap(({ list }) => {
    const re = new RegExp(`^[(${(list || []).concat('*').join('|')})] log:`);
    socket.messages.pipe(
      filter(re.test)
    )
  }),
  map(LogFromBoardAction)
);

reducerRegistry.register(storeName, initalState, reducer);

registerEpic(initEpic);
registerEpic(logEpic);

const boardsStoreSelector = state => state[storeName];
const boardsListSelector = state => state[storeName].boards;
const boardsLogSelector = state => state[storeName].logs;


const boardNamesSelector = createSelector(
  boardsListSelector,
  b => b.boards.map(b => b.name)
);

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
