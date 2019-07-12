import { allConfigurationsSelector, allConfNamesSelector } from '../boards/store';
import { createSelector } from 'reselect';
import { connect } from 'react-redux';
import { reducerRegistry } from '../../store/utils';

const FieldName = 'ConfNodes';
const ADD_NODE_TYPES = `${FieldName}: Add registered node types`;
export const AddNodeTypesAction = nodes => ({type: ADD_NODE_TYPES, nodes});

const anc = (x, y) => {
  const arr = x || [];
  const len = arr.length;
  return arr.map((x, i) => ({...x, anchor: [(i + 1) / (len + 1), y]}));
};

const anchors = ({inputs, outputs}) => {
  const all = [...anc(inputs, 0), ...anc(outputs, 1)];
  return {
    connections: all,
    anchors: all.map(({anchor}) => anchor)
  };
};

reducerRegistry.register(FieldName, {}, {
  [ADD_NODE_TYPES]: (state, {nodes}) => ({...state, nodes: nodes.map(x => ({...x, ...anchors(x)}))})
});

const getState = state => state[FieldName] || {};
const getIndex = (state, {index}) => index;
const nodesSelector = createSelector(getState, s => s.nodes || []);
const countSelector = createSelector(nodesSelector, n => n.length);
const nodeSelectorFactory = () => createSelector(nodesSelector, getIndex, (n, i) => n[i]);
const mapNodesCountToProps = (state, props) => ({
  count: countSelector(state, props)
});
const mapSetNodeTypesToProps = dispatch => ({
  onData: data => dispatch(AddNodeTypesAction(data))
});
export const connectNodesList = connect(mapNodesCountToProps, mapSetNodeTypesToProps);

const mapNodeToProps = () => {
  const nodeSelector = nodeSelectorFactory();
  return (state, props) => ({
    node: nodeSelector(state, props)
  })
};
export const connectNode = connect(mapNodeToProps, () => ({}));

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
