import { connect } from 'react-redux';
import './epics';
import {
  monitorsBufferState,
  monitorsState,
  mapStateToPropsList,
  noop,
  mapValueStateToProps
} from './selectors';

export const connectMonitorsContainer = connect(
  mapStateToPropsList,
  noop
);

export const connectMonitorValue = connect(
  mapValueStateToProps(monitorsState),
  noop
);
export const connectMonitorValueSeconds = connect(
  mapValueStateToProps(monitorsBufferState),
  noop
);