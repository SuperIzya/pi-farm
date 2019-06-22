import { allConfigurationsSelector, allConfNamesSelector } from '../boards/store';
import { createSelector } from 'reselect';
import { connect } from 'react-redux';

export const getConf = (state, props) => ((props.match || {}).params || {}).name || '';
export const confSelector = createSelector(allConfigurationsSelector, getConf, (a, c) => {
  return a[c] || {}
});

const mapListToProps = state => ({
  names: allConfNamesSelector(state)
});

export const connectList = connect(mapListToProps, () => ({}));

const mapConfToProps = (state, props) => ({
  configuration: confSelector(state, props)
});

export const connectConfiguration = connect(mapConfToProps, () => ({}));
