import { createSelector } from 'reselect';
import {
  MonitorBufferState,
  MonitorState
} from './store';

export const monitorsBufferState = state => state[MonitorBufferState];
export const monitorsState = state => state[MonitorState];
export const monitorNamesSelector = createSelector(
  monitorsBufferState,
  Object.keys
);
export const getValues = (state, monitor) => state[monitor] || [];
export const getMonitorValue = (state, props) => {
  const values = getValues(state, props.monitor);
  if (values.length > props.index) return values[props.index];
  return null;
};
export const monitorSelector = stateSelector => createSelector(
  stateSelector,
  getMonitorValue
);

export const mapStateToPropsList = state => ({
  monitors: monitorNamesSelector(state)
});

export const mapValueStateToProps = (stateSelector) => (state, props) => ({
  value: monitorSelector(stateSelector)(state, props)
});
export const noop = () => ({});