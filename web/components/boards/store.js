import { createSelector } from 'reselect';
import { connect } from 'react-redux';
import { reducerRegistry } from '../../store/utils';
import {
  SET_BOARD_VALUE,
} from '../../store/actions';
import _ from 'lodash';

export const storeName = "Boards";

export const INIT_BOARDS = "Init boards";
export const InitBoardsAction = () => ({ type: INIT_BOARDS });

export const SET_BOARD_SELECTED = "Select board";
export const SetBoardSelectedAction = device => ({ type: SET_BOARD_SELECTED, device });

export const CLEAR_BOARD_SELECTION = "Clear selected board";
export const ClearBoardSelection = () => ({ type: CLEAR_BOARD_SELECTION });

export const SET_BOARDS_LIST = "Set boards list";
export const SetBoardsListAction = devices => ({ type: SET_BOARDS_LIST, devices });

export const SET_DRIVERS_LIST = "Set drivers list";
export const SetDriversListAction = drivers => ({ type: SET_DRIVERS_LIST, drivers });

export const REQ_DRIVER_ASSIGNATION = "Request driver assignation";
export const ReqDriverAssignationAction = (device, driver) => ({ type: REQ_DRIVER_ASSIGNATION, device, driver });

export const SET_DRIVER_ASSIGNATION = "Set driver assignation";
export const SetDriverAssignationAction = (device, driver) => ({ type: SET_DRIVER_ASSIGNATION, device, driver })

export const SEND_TO_DRIVER = "Send to driver";
export const SendToDriverAction = (message, device) => ({ type: SEND_TO_DRIVER, message, device });

export const UPDATE_FROM_DRIVER = "Received from driver";
export const UpdateFromDriverAction = (device, driver, data) => ({ type: UPDATE_FROM_DRIVER, data, device, driver });

export const SET_CONFIGURATION_LIST = "Set configuration list";
export const SetConfigurationListAction = configurations => ({ type: SET_CONFIGURATION_LIST, configurations });

export const REQ_CONFIGURATIONS_UPDATE = "Request configurations update";
export const ReqConfigurationsUpdate = (device, driver, configurations) => ({
  type: REQ_CONFIGURATIONS_UPDATE,
  driver,
  device,
  configurations
});

export const REQ_CONFIGURATION_REMOVE = 'Request cofniguration remove';
export const ReqConfigurationRemove = (device, driver, configurations) => ({
  type: REQ_CONFIGURATION_REMOVE,
  driver,
  device,
  configurations
});

export const SET_CONFIGURATIONS = "Set configurations per device";
export const SetConfigurationsAction = (device, configurations) => ({
  type: SET_CONFIGURATIONS,
  device,
  configurations
});

const getDevice = (state, device) => (state.boards || {})[device];
const reducer = {
  [SET_BOARD_VALUE]: (state, { board, value }) => {
    if (_.isEqual((state[board] || {}).value, value)) return state;
    return { ...state, [board]: { ...state[board], value } };
  },
  [SET_BOARDS_LIST]: (state, { devices }) => ({
    ...state,
    boards: devices.map(name => ({
      [name]: {
        ...getDevice(state, name),
        name
      }
    })).reduce((a, b) => ({ ...a, ...b }), {})
  }),
  
  [SET_DRIVERS_LIST]: (state, { drivers }) => ({ ...state, drivers }),
  [SET_CONFIGURATION_LIST]: (state, { configurations }) => ({ ...state, configurations }),
  
  [UPDATE_FROM_DRIVER]: (state, { data, driver, device }) => !getDevice(state, device) ? state : ({
    ...state,
    boards: {
      ...state.boards,
      [device]: {
        ...state.boards[device],
        data: {
          ...(state.boards[device].data || {}),
          [data.type]: data
        },
        driver,
      }
    }
  }),
  [SET_DRIVER_ASSIGNATION]: (state, { device, driver }) => !getDevice(state, device) ? state : ({
    ...state,
    boards: {
      ...state.boards,
      [device]: {
        ...state.boards[device],
        driver
      }
    }
  }),
  [SET_CONFIGURATIONS]: (state, { device, configurations }) => !getDevice(state, device)
    ? state
    : {
      ...state,
      boards: {
        ...state.boards,
        [device]: {
          ...state.boards[device],
          configurations
        }
      }
    },
  [SET_BOARD_SELECTED]: (state, { device }) => {
    const selected = state.selected || {};
    const boards = {
      ...state.boards,
      ...(selected.name ? {
        [selected.name]: {
          ...state.boards[selected.name],
          selected: false
        }
      } : {}),
      [device]: {
        ...state.boards[device],
        selected: true
      }
    };
    return {
      ...state,
      boards,
      selected: boards[device]
    }
  },
  [CLEAR_BOARD_SELECTION]: state => {
    const selected = state.selected || {};
    if (selected.name) return {
      ...state,
      selected: {},
      boards: {
        ...state.boards,
        [selected.name]: {
          ...state.boards[selected.name],
          selected: false
        }
      }
    };
    return state;
  }
  
};

reducerRegistry.register(storeName, {}, reducer);

export const getState = state => state[storeName] || {};
export const selectedSelector = createSelector(getState, s => s.selected || {});
export const boardsSelector = createSelector(getState, s => s.boards || {});
export const driversSelector = createSelector(getState, s => s.drivers || []);
export const boardNamesSelector = createSelector(
  boardsSelector,
  b => Object.keys(b)
);

const mapStateToProps = state => ({
  boardNames: boardNamesSelector(state),
  drivers: driversSelector(state)
});

const mapDispatchToProps = dispatch => ({
  initBoards: () => dispatch(InitBoardsAction())
});

export const connectToBoards = connect(mapStateToProps, mapDispatchToProps);

export const getKey = (s, props) => props.device;
export const deviceSelectorFactory = () => createSelector(boardsSelector, getKey, (b, k) => b[k] || {});
export const isSelectedSelectorFactory = deviceSelector => createSelector(deviceSelector, d => !!d.selected);
export const driverNameFactory = deviceSelector => createSelector(deviceSelector, d => d.driver || '');
export const metaSelectorFactory = deviceSelector => createSelector(
  deviceSelector,
  driversSelector,
  (device, drivers) => {
    const index = drivers.findIndex(d => d.name === device.driver);
    return index < 0 ? {} : drivers[index].meta;
  }
);
export const dataSelectorFactory = ds => createSelector(ds, d => d.data || {});
export const miniBoardSelectorFactory = metaSelector => createSelector(
  metaSelector, m => m.mini
);
export const indexBoardSelectorFactory = metaSelector => createSelector(
  metaSelector, m => m.index
);


const mapBoardDispatchToProps = (dispatch, props) => ({
  send: msg => !_.isEmpty(msg) && dispatch(SendToDriverAction(msg, props.device)),
});

const mapDataToProps = () => {
  const deviceSelector = deviceSelectorFactory();
  const dataSelector = dataSelectorFactory(deviceSelector);
  return (state, props) => ({
    data: dataSelector(state, props)
  })
};

export const connectBoardData = connect(mapDataToProps, () => ({}));

const mapInnerBoardStateToProps = () => {
  
  const deviceSelector = deviceSelectorFactory();
  const driverSelector = driverNameFactory(deviceSelector);
  const metaSelector = metaSelectorFactory(deviceSelector);
  const miniSelector = miniBoardSelectorFactory(metaSelector);
  const indexSelector = indexBoardSelectorFactory(metaSelector);
  return (state, props) => ({
    driver: driverSelector(state, props),
    index: indexSelector(state, props),
    mini: miniSelector(state, props),
  })
};


export const connectInnerBoard = connect(mapInnerBoardStateToProps, mapBoardDispatchToProps);

const mapBoardFrameStateToProps = () => {
  const deviceSelector = deviceSelectorFactory();
  const isSelectedSelector = isSelectedSelectorFactory(deviceSelector);
  return (state, props) => ({
    selected: isSelectedSelector(state, props)
  })
};

export const connectBoardFrame = connect(mapBoardFrameStateToProps, () => ({}));

const mapBoardFooterDispatchToProps = (dispatch, props) => ({
  assignDriver: driver => !_.isEmpty(driver) && dispatch(ReqDriverAssignationAction(props.device, driver.label))
});

export const connectBoardFooter = connect(() => ({}), mapBoardFooterDispatchToProps);

export const mapMiniBoardStateToProps = () => {
  const deviceSelector = deviceSelectorFactory();
  const driverSelector = driverNameFactory(deviceSelector);
  return (state, props) => ({
    driver: driverSelector(state, props)
  })
};

export const connectMiniBoard = connect(mapMiniBoardStateToProps, mapBoardDispatchToProps);

export const allConfigurationsSelector = createSelector(getState, s => s.configurations || {});
export const allConfNamesSelector = createSelector(allConfigurationsSelector, c => Object.keys(c) || []);
const configurationsSelectorFactory = deviceSelector => createSelector(
  deviceSelector,
  d => d.configurations || []);


const mapConfigSelStateToProps = () => {
  const deviceSelector = deviceSelectorFactory();
  const configSelector = configurationsSelectorFactory(deviceSelector);
  return (state, props) => ({
    confNames: allConfNamesSelector(state),
    configurations: configSelector(state, props)
  });
};

const mapDispatchToConfigSelectorProps = () => (dispatch, { device, driver }) => ({
  selectConfigs: configurations => dispatch(ReqConfigurationsUpdate(
    device,
    driver,
    configurations
  )),
  removeConfig: configurations => dispatch(ReqConfigurationRemove(
    device,
    driver,
    configurations
  ))
})
;

export const connectConfigurationSelector = connect(mapConfigSelStateToProps, mapDispatchToConfigSelectorProps);
