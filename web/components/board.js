import { connect } from 'react-redux';
import { logFiltersSelector } from './log.js';
import { boardsSelector } from './boards.js';
import { createSelector } from 'reselect';
import { ToggleLogFilterAction } from '../store/actions';


const getName = (state, props) => props.board;
const getBoard = createSelector(
  boardsSelector,
  getName,
  (boards, name) => boards[name]
);

const createBoardValueSelector = sensor => createSelector(
  getBoard,
  board => (board.value && board.value[sensor]) || 0
);
const createBoardLogStatusSelector = () => createSelector(
  logFiltersSelector,
  getName,
  (filters, name) => filters.indexOf(name) > -1
);
const mapLogStateToProps = () => (state, props) => ({
  isOn: createBoardLogStatusSelector()(state, props),
});

const mapValueToProps = () => (state, props) => ({
  value: createBoardValueSelector(props.sensor)(state, props) || 0
});

const mapDispatchToProps = (dispatch, { board }) => ({
  toggleFilter: () => dispatch(ToggleLogFilterAction(board))
});

const connectBoard = connect(mapLogStateToProps, mapDispatchToProps);
const connectHand = connect(mapValueToProps, () => ({}));

export {
  connectBoard,
  connectHand
};
