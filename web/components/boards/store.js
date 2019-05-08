import {
  mergeMap,
  tap,
  map,
  filter,
  distinctUntilChanged,
  groupBy,
  takeUntil,
  withLatestFrom
} from 'rxjs/operators';
import { of } from 'rxjs';
import { createSelector } from 'reselect';
import { connect } from 'react-redux';
import { ofType } from 'redux-observable';
import { reducerRegistry } from '../../store/utils';
import { registerEpic } from '../../store/epics';
import {
  SET_BOARD_VALUE,
  setBoardValue,
} from '../../store/actions';
import socket from '../../utils/socket';
import _ from 'lodash';

const storeName = "Boards";

export const INIT_BOARDS = "Init boards";
export const InitBoardsAction = () => ({ type: INIT_BOARDS });

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

const reducer = {
  [SET_BOARD_VALUE]: (state, { board, value }) => {
    if (_.isEqual((state[board] || {}).value, value)) return state;
    return { ...state, [board]: { ...state[board], value } };
  },
  [SET_BOARDS_LIST]: (state, { devices }) => ({
    ...state,
    boards: devices.map(name => ({ [name]: { name } })).reduce((a, b) => ({ ...a, ...b }), {})
  }),
  [SET_DRIVERS_LIST]: (state, { drivers }) => ({ ...state, drivers }),
  [UPDATE_FROM_DRIVER]: (state, { data, driver, device }) => !(state.boards || {})[device] ? state : ({
    ...state,
    boards: {
      ...state.boards,
      [device]: {
        ...state.boards[device],
        driver,
        data
      }
    }
  }),
  [SET_DRIVER_ASSIGNATION]: (state, { device, driver }) => !(state.boards || {})[device] ? state : ({
    ...state,
    boards: {
      ...state.boards,
      [device]: {
        ...state.boards[device],
        driver
      }
    }
  })
};

reducerRegistry.register(storeName, {}, reducer);

const re = /^\[([^\]]+)\] value: (-?\d+(\.\d+)?) - (-?\d+(\.\d+)?) - (-?\d+(\.\d+)?) - (\d+)(.*)$/i;
registerEpic(action$ => action$.pipe(
  ofType(INIT_BOARDS),
  mergeMap(() => socket.messages.pipe(
    filter(log => / value: /.test(log)),
    map(_.memoize(log => {
      const matches = log.match(re);
      return setBoardValue(
        matches[1],
        parseFloat(matches[2]),
        parseFloat(matches[4]),
        parseFloat(matches[6]),
        parseInt(matches[8]));
    })),
    groupBy(v => v.board),
    mergeMap(g => g.pipe(
      distinctUntilChanged()
    ))
  ))
));


export const getState = state => state[storeName] || {};
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

const getKey = (s, props) => props.name;
const deviceSelectorFactory = () => createSelector(boardsSelector, getKey, (b, k) => b[k] || {});
const driverNameFactory = deviceSelector => createSelector(deviceSelector, d => d.driver || '');
const metaSelectorFactory = deviceSelector => createSelector(
  deviceSelector,
  driversSelector,
  (device, drivers) => {
    const index = drivers.findIndex(d => d.name === device.driver);
    return index < 0 ? {} : drivers[index].meta;
  }
);


const mapBoardStateToProps = (state, props) => {
  const deviceSelector = deviceSelectorFactory();
  const metaSelector = metaSelectorFactory(deviceSelector);
  const driverSelector = driverNameFactory(deviceSelector);
  return {
    driver: driverSelector(state, props),
    device: props.name,
    meta: metaSelector(state, props)
  }
};

const mapBoardDispatchToProps = (dispatch, props) => ({
  send: msg => !_.isEmpty(msg) && dispatch(SendToDriverAction(msg, props.name)),
  assignDriver: driver => !_.isEmpty(driver) && dispatch(ReqDriverAssignationAction(props.name, driver.label))
});

export const connectBoard = connect(mapBoardStateToProps, mapBoardDispatchToProps);

export const registerBoardEpics = (stop) => {
  const deviceSelector = deviceSelectorFactory();
  const driverSelector = driverNameFactory(deviceSelector);
  
  const getDriver = (state, {device}) => driverSelector(state, {name: device});
  
  registerEpic((action$, state$) => action$.pipe(
    takeUntil(stop),
    ofType(REQ_DRIVER_ASSIGNATION),
    filter(({ driver }) => !!driver),
    withLatestFrom(state$),
    filter(([a, state]) => getDriver(state, a) !== a.driver),
    map(({ device, driver }) => {
      socket.send({
        type: 'assign-driver',
        device,
        driver
      });
      return false;
    }),
    filter(Boolean)
  ));
  
  registerEpic((action$, state$) => action$.pipe(
    takeUntil(stop),
    ofType(SEND_TO_DRIVER),
    withLatestFrom(state$),
    map(([a, state]) => {
      
      const driver = getDriver(state, a);
      socket.send({
        driver,
        deviceId: a.device,
        data: a.message,
        type: 'to-device'
      });
      return false;
    }),
    filter(Boolean)
  ));
  
  registerEpic(action$ => action$.pipe(
    ofType(INIT_BOARDS),
    mergeMap(() => {
      socket.send({ type: 'get-drivers-state' });
      socket.send({ type: 'get-devices' });
      return socket.messages.pipe(
        tap(x => console.log('before ofType', x)),
        ofType('drivers', 'devices', 'connectors', 'from-device'),
        tap(x => console.log(x))
      )
    }),
    mergeMap(x => {
      switch (x.type) {
        case 'drivers':
          return of(SetDriversListAction(x.drivers));
        case 'devices':
          return of(SetBoardsListAction(x.devices));
        case 'connectors':
          return Object.keys(x.drivers).map(k =>
            SetDriverAssignationAction(k, x.drivers[k])
          );
        case 'from-device':
          return of(UpdateFromDriverAction(x.deviceId, x.driver, x.data))
      }
    }),
    takeUntil(stop)
  ))
};

