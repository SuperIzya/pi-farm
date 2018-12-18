import { reducerRegistry } from '../../store/utils';

export const MonitorState = "Monitor";
export const MonitorBufferState = "MonitorBuffer";
export const ADD_MONITOR_DATA = "Add monitor data";
export const CLEAR_MONITOR_BUFFER = "Clear monitor buffer";
export const ADD_COMPRESSED_MONITOR_DATA = "Add compressed monitor data";
export const MonitorData = (monitor, interval, total) => type => ({
  type,
  monitor,
  data: {
    interval,
    total
  }
});
export const ClearMonitorBuffer = monitor => ({ type: CLEAR_MONITOR_BUFFER, monitor });
export const HistorySpan = 60;

const addDataReducer = slice => {
  if (!slice) {
    return (state, { monitor, data }) => ({
      ...state,
      [monitor]: [ data, ...(state[monitor] || [])]
    });
  }
  return (state, { monitor, data }) => {
    const array = state[monitor];
    var newArray;
    if (array.length > HistorySpan * 1.2)
      newArray = array.slice(0, HistorySpan);
    else newArray = array;
    return {
      ...state,
      [monitor]: [data, ...newArray]
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
