import { reducerRegistry } from '../store/utils';
import { createSelector } from 'reselect';
import { connect } from 'react-redux';
import { ofType } from 'redux-observable';
import { registerEpic } from '../store/epics';

import { timer, from } from 'rxjs';
import { INIT_BOARDS } from '../store/actions';
import socket from '../utils/socket';

const MonitorState = "Monitor";
const MonitorBufferState = "MonitorBuffer";
const ADD_MONITOR_DATA = "Add monitor data";
const CLEAR_MONITOR_BUFFER = "Clear monitor buffer";
const ADD_COMPRESSED_MONITOR_DATA = "Add compressed monitor data";
const MonitorData = (monitor, interval, total) => type => ({
  type,
  monitor,
  data: {
    interval,
    total
  }
});
const ClearMonitorBuffer = monitor => ({ type: CLEAR_MONITOR_BUFFER, monitor });
const HistorySpan = 60;

const addDataReducer = slice => {
  if (!slice) {
    
    return (state, { monitor, data }) => ({
      ...state,
      [monitor]: [...(state[monitor] || []), data]
    });
  }
  return (state, { monitor, data }) => {
    const array = state[monitor];
    if (array.length > HistorySpan) {
      var newArray = array.concat([data]);
      return {
        ...state,
        [monitor]: newArray.slice(newArray.length - HistorySpan)
      }
    }
  };
};

reducerRegistry.register(MonitorBufferState, {}, {
  [ADD_MONITOR_DATA]: addDataReducer(false),
  [CLEAR_MONITOR_BUFFER]: (state, { monitor }) => ({ ...state, [monitor]: [] })
});
reducerRegistry.register(MonitorState, {}, {
  [ADD_COMPRESSED_MONITOR_DATA]: addDataReducer(true)
});

const monitorsBufferState = state => state[MonitorBufferState];
const monitorsState = state => state[MonitorState];
const monitorNamesSelector = createSelector(
  monitorsBufferState,
  Object.keys
);
const getMonitorName = (state, props) => props.monitor;
const getValues = (state, monitor) => state[monitor] || [];
const getMonitorValue = (state, props) => {
  const values = getValues(state, props.monitor);
  if (values.length > props.index) return values[props.index];
  return null;
};
const monitorBufferSelector = () => createSelector(
  monitorsBufferState,
  getMonitorValue
);
const monitorSelector = () => createSelector(
  monitorsState,
  getMonitorValue
);

const monitorValuesLengthSelector = () => createSelector(
  monitorsState,
  getMonitorName,
  (state, monitor) => getValues(state, monitor).length
);

const mapStateToPropsList = state => ({
  monitors: monitorNamesSelector(state)
});
const mapMonitorStateToProps = (state, props) => ({
  length: monitorValuesLengthSelector()(state, props)
});

const mapValueStateToProps = (state, props) => ({
  value: monitorSelector()(state, props)
});
export const connectMonitorsContainer = connect(
  mapStateToPropsList,
  {}
);

export const connectMonitorValue = connect(
  mapValueStateToProps,
  {}
);

export const connectMonitor = connect(
  mapMonitorStateToProps,
  {}
);

const monitorRe = /^mon: (\d+) (\d+) (.+)$/i;
const monitorEpic = action$ => action$.pipe(
  ofType(INIT_BOARDS),
  mergeMap(() => socket.messages.pipe(
    filter(log => /^mon: /.test(log)),
    map(l => {
      const match = l.match(monitorRe);
      return MonitorData(match[3], parseInt(match[2]), parseInt(match[1]))(ADD_MONITOR_DATA);
    })
  ))
);

import {
  mergeMap,
  map,
  filter,
  
} from 'rxjs/operators'

registerEpic(monitorEpic);
const timeout = HistorySpan * 1000;
const compressEpic = (action$, state) => action$.pipe(
  ofType(INIT_BOARDS),
  mergeMap(() => timer(timeout, timeout)),
  mergeMap(() => monitorNamesSelector(state)),
  mergeMap(monitor => {
    const state = monitorsBufferState(state)[monitor];
    if (state.length > HistorySpan) {
      const count = state.reduce((acc, v) => ({
        total: acc.total + v.total,
        interval: acc.interval + v.interval
      }), { total: 0, interval: 0 });
      
      return [
        ClearMonitorBuffer(monitor),
        MonitorData(monitor, count.interval, count.total)(ADD_COMPRESSED_MONITOR_DATA)
      ];
    }
  })
);

registerEpic(compressEpic);
