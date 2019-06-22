import {allConfNamesSelector} from '../boards/store';
import { createSelector } from 'reselect';
import { connect } from 'react-redux';

export const getConf = (state, props) => props.name || '';
export const confSelector = createSelector(allConfNamesSelector, getConf, (a, c) => a[c] || {});

const mapListToProps = state => ({
  names: allConfNamesSelector(state)
});

export const connectList = connect(mapListToProps, () => ({}));
