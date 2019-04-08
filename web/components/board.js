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
const mapLogStateToProps = () => {
  const statusSelector = createBoardLogStatusSelector();
  return (state, props) => ({
    isOn: statusSelector(state, props),
  });
};

const mapValueToProps = () => {
  const selector = createBoardValueSelector(props.sensor);
  return (state, props) => ({
    value: selector(state, props)
  });
};

const mapDispatchToProps = (dispatch, { board }) => ({
  toggleFilter: () => dispatch(ToggleLogFilterAction(board))
});

const connectBoard = connect(mapLogStateToProps, mapDispatchToProps);
const connectHand = connect(mapValueToProps);

export {
  connectBoard,
  connectHand
};
