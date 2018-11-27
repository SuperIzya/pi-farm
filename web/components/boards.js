import { mergeMap, map, pluck } from 'rxjs/operators'
import { createSelector } from 'reselect';
import { connect } from 'react-redux';
import { ofType } from 'redux-observable';
import Client from '../utils/client';
import { reducerRegistry } from '../store/utils';
import { registerEpic } from '../store/epics';

const storeName = "Boards";

const INIT_BOARDS = "Init boards";
const InitBoardsAction = () => ({ type: INIT_BOARDS });

const SET_BOARDS_LIST = "Set boards list";
const SetBoardsListAction = list => ({ type: SET_BOARDS_LIST, list });


const initialState = [];

const reducer = {
  [SET_BOARDS_LIST]: (state, { list }) => list.map(name => ({ name })),
};

const initEpic = action$ => action$.pipe(
  ofType(INIT_BOARDS),
  mergeMap(() => Client.get('boards').pipe(
    pluck('data')
  )),
  map(SetBoardsListAction)
);

reducerRegistry.register(storeName, initialState, reducer);

registerEpic(initEpic);

const boardsListSelector = state => state[storeName];


const boardNamesSelector = createSelector(
  boardsListSelector,
  b => b.map(b => b.name)
);

const mapStateToProps = state => ({
  boardNames: boardNamesSelector(state)
});

const mapDispatchToProps = dispatch => ({
  initBoards: () => dispatch(InitBoardsAction())
});

const connectToBoards = connect(mapStateToProps, mapDispatchToProps);

export {
  INIT_BOARDS,
  InitBoardsAction,
  boardNamesSelector,
  connectToBoards
};
