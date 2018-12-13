import { mergeMap, map, pluck, filter } from 'rxjs/operators'
import { createSelector } from 'reselect';
import { connect } from 'react-redux';
import { ofType } from 'redux-observable';
import Client from '../utils/client';
import { reducerRegistry } from '../store/utils';
import { registerEpic } from '../store/epics';
import {
  INIT_BOARDS,
  InitBoardsAction,
  SET_BOARDS_LIST,
  SET_BOARD_VALUE,
  SetBoardsListAction,
  setBoardValue,
} from '../store/actions';
import socket from '../utils/socket';

const storeName = "Boards";


const reducer = {
  [SET_BOARD_VALUE]: (state, { board, value }) => ({ ...state, [board]: { ...state[board], value } }),
  [SET_BOARDS_LIST]: (state, { list }) => list.map(name => ({ [name]: { name } })).reduce((a, b) => ({ ...a, ...b }), {}),
};

reducerRegistry.register(storeName, {}, reducer);

const initEpic = action$ => action$.pipe(
  ofType(INIT_BOARDS),
  mergeMap(() => Client.get('boards').pipe(
    pluck('data')
  )),
  map(SetBoardsListAction)
);

registerEpic(action$ => action$.pipe(
  ofType(INIT_BOARDS),
  mergeMap(() => socket.messages.pipe(
    filter(log => / value: /.test(log))
  )),
  map(log => {
    const matches = log.match(/^\[([^\]]+)\] value: (\d+(\.\d+)?) - (\d+(\.\d+)?) - (\d+)$/i);
    return setBoardValue(matches[1], parseFloat(matches[2]), parseFloat(matches[4]), parseInt(matches[6]));
  }),
  filter(Boolean)
));
registerEpic(initEpic);

const boardsSelector = state => state[storeName];
const boardNamesSelector = createSelector(
  boardsSelector,
  b => Object.keys(b)
);

const mapStateToProps = state => ({
  boardNames: boardNamesSelector(state)
});

const mapDispatchToProps = dispatch => ({
  initBoards: () => dispatch(InitBoardsAction)
});

const connectToBoards = connect(mapStateToProps, mapDispatchToProps);

export {
  boardsSelector,
  boardNamesSelector,
  connectToBoards
};
