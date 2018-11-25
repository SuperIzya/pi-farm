import { connect } from 'react-redux';
import { reducerRegistry } from '../store/utils';
import { createSelector } from 'reselect';
import { single, take, concatAll, toArray, filter, mergeMap, map } from 'rxjs/operators';
import { registerEpic } from '../store/epics';
import { ofType } from 'redux-observable';
import { INIT_BOARDS } from './boards.js';
import socket from '../utils/socket';

const storeName = 'Logs';

const ADD_LOG = 'Add log';
const addLogAction = (log) => ({ type: ADD_LOG, log });

const CLEAR_LOG = 'Clear log';
const clearLogAction = { type: CLEAR_LOG };

const SET_MAX_LOGS = "Set max logs";
const setMaxLogsAction = maxLogs => ({ type: SET_MAX_LOGS, maxLogs });

const TOGGLE_LOG_FILTER = "Toggle log filter";
const toggleLogFilterAction = board => ({ type: TOGGLE_LOG_FILTER, board });

const initalState = {
  maxLogs: 100,
  logs: [],
  filters: ['*']
};

const addLog = (array, log, max) => single(log).pipe(
  concatAll(array),
  take(max),
  toArray()
);

const toggleLogFilter = (filters, toggle) => {
  const index = filters.indexOf(toggle);
  
  if (index < 0) return [...filters, toggle];
  
  return filters.slice(0, index).concat(
    filters.slice(index, filters.length)
  );
};

const reducer = {
  [CLEAR_LOG]: state => ({ ...state, logs: [] }),
  [SET_MAX_LOGS]: (state, { maxLogs }) => ({ ...state, maxLogs }),
  [ADD_LOG]: (state, { log }) => ({ ...state, logs: addLog(state.logs, log, state.maxLogs) }),
  [TOGGLE_LOG_FILTER]: (state, { board }) => ({ ...state, filters: toggleLogFilter(state.filters, board) }),
};

reducerRegistry.register(storeName, initalState, reducer);

const logsStoreSelector = state => state[storeName];

const logsListSelector = state => logsStoreSelector(state).logs;
const maxSelector = state => logsStoreSelector(state).maxLogs;
const logFiltersSelector = state => logsStoreSelector(state).filters;

const logsSelector = createSelector(
  logsListSelector,
  logFiltersSelector,
  maxSelector,
  (logs, filters, max) => {
    const re = new RegExp(`^\[(${filters.join('|')})\]`);
    return logs.filter(re.test).slice(0, max);
  }
);

const mapStateToProps = state => ({
  logs: logsSelector(state),
  filters: logFiltersSelector(state)
});

const mapDispatchToProps = dispatch => ({
  clearLogs: () => dispatch(clearLogAction),
  setMaxLogs: max => dispatch(setMaxLogsAction(max))
});

const connectToLog = connect(mapStateToProps, mapDispatchToProps);

const logEpic = action$ => action$
.pipe(
  ofType(INIT_BOARDS),
  mergeMap(() => {
    const re = /^\[[^\]]+\] log:/;
    return socket.messages.pipe(
      filter(re.test)
    )
  }),
  map(addLogAction)
);

registerEpic(logEpic);

export {
  addLogAction,
  setMaxLogsAction,
  toggleLogFilterAction,
  logFiltersSelector,
  logsSelector,
  connectToLog
};

