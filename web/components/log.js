import {connect} from 'react-redux';
import {reducerRegistry} from '../store/utils';
import { createSelector } from 'reselect';
const storeName = 'log';
const ADD_LOG = 'Add log';
const CLEAR_LOG = 'Clear log';
const CHANGE_LOG_DEPTH = 'Change log depth';

const cutLogs = (state) => {
  if(state.logs.length >= state.depth)
    return state.logs.slice(0, state.depth - 1);
  return state.logs;
};

reducerRegistry.register(storeName, { depth: 10, logs: [] }, {
  [ADD_LOG]: (state, action) => ({...state, logs: [action.log, ...cutLogs(state) ]}),
  [CHANGE_LOG_DEPTH]: (state, action) => ({...state, depth: action.depth}),
  [CLEAR_LOG]: state => ({...state, logs: []}),
});

const addLogAction = (log) => ({ type: ADD_LOG, log });
const changeLogDepthAction = (depth) => ({type: CHANGE_LOG_DEPTH, depth });
const clearLogAction = {type: CLEAR_LOG};
const getLogState = state => state.log;

const logsSelector = createSelector(
  [getLogState],
  logState => logState.logs
);


const mapStateToProps = state => ({
  logs: logsSelector(state)
});

const mapDispatchToProps = dispatch => ({
  clearLogs: () => dispatch(clearLogAction)
});

const connectToLog = connect(mapStateToProps, mapDispatchToProps);

export {
  addLogAction,
  changeLogDepthAction,
  logsSelector,
  connectToLog
};

