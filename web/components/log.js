import { connect } from 'react-redux';
import { reducerRegistry } from '../store/utils';
import { createSelector } from 'reselect';
import { filter, mergeMap, map } from 'rxjs/operators';
import { registerEpic } from '../store/epics';
import { ofType } from 'redux-observable';
import socket from '../utils/socket';
import {
  ADD_LOG, addLogAction,
  CLEAR_LOG,
  clearLogAction,
  INIT_BOARDS,
  SET_MAX_LOGS,
  setMaxLogsAction,
  TOGGLE_LOG_FILTER
} from '../store/actions';

const storeName = 'Logs';

const initalState = {
  maxLogs: 50,
  logs: [],
  filters: ['*']
};

const addLog = (array, log, max) => [{ timestamp: Date.now(), log }, ...array].slice(0, max);

const toggleLogFilter = (filters, toggle) => {
  const index = filters.indexOf(toggle);
  
  if (index < 0) return [...filters, toggle];
  
  return filters.slice(0, index).concat( filters.slice(index + 1) );
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

const filterLogs = (logs, filters) => {
  const re = new RegExp(`\[(${filters.join('|')})\]`);
  return logs.filter(x => re.test(x.log));
};

const logsSelector = createSelector(
  logsListSelector,
  logFiltersSelector,
  maxSelector,
  (logs, filters, max) => filterLogs(logs, filters).slice(0, max)
);

const mapFilterStateToProps = state => ({
  filters: logFiltersSelector(state),
  maxLogs: maxSelector(state)
});

const mapLogsStateToProps = state => ({
  logs: logsSelector(state)
});

const mapDispatchToProps = dispatch => ({
  clearLogs: () => dispatch(clearLogAction),
  setMaxLogs: max => dispatch(setMaxLogsAction(max))
});

const connectToLog = connect(mapFilterStateToProps, mapDispatchToProps);
const connectToLogList = connect(mapLogsStateToProps, null);

const logEpic = action$ => action$
.pipe(
  ofType(INIT_BOARDS),
  mergeMap(() => {
    const re = /^\[[^\]]+\] log:/;
    return socket.messages.pipe(
      filter(x => re.test(x))
    )
  }),
  map(addLogAction)
);

registerEpic(logEpic);

export {
  logFiltersSelector,
  filterLogs,
  logsSelector,
  connectToLog,
  connectToLogList
};

