import { ofType } from 'redux-observable';
import { registerEpic } from '../../store/epics';
import { timer } from 'rxjs';
import socket from '../../utils/socket';
import { INIT_BOARDS } from '../../store/actions';
import {
mergeMap,
map,
filter,

} from 'rxjs/operators'
import {
  monitorNamesSelector,
  monitorsBufferState
} from './selectors';
import {
ADD_COMPRESSED_MONITOR_DATA,
ADD_MONITOR_DATA,
ClearMonitorBuffer,
HistorySpan,
MonitorData
} from './store';

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


registerEpic(monitorEpic);
const timeout = HistorySpan * 1000;
const compressEpic = (action$, state) => action$.pipe(
  ofType(INIT_BOARDS),
  mergeMap(() => timer(timeout, timeout)),
  mergeMap(() => monitorNamesSelector(state.value)),
  mergeMap(monitor => {
    const state = monitorsBufferState(state.value)[monitor];
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
