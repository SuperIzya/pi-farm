import {connect} from 'react-redux';
import { logFiltersSelector, toggleLogFilterAction } from './log.js';

const mapLogStateToProps = (state, props) => ({
  isOn: logFiltersSelector(state).indexOf(props.board) >= 0
});

const mapDispatchToProps = (dispatch, { board }) => ({
  toggleFilter: () => dispatch(toggleLogFilterAction(board))
});

const connectBoard = connect(mapLogStateToProps, mapDispatchToProps);

export {
  connectBoard
};
