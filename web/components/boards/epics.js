import { registerEpic } from '../../store/epics';
import { filter, map, mapTo, mergeMap, takeUntil, withLatestFrom } from 'rxjs/operators';
import { ofType } from 'redux-observable';
import socket from '../../utils/socket';
import { EMPTY, merge, of, from } from 'rxjs';
import {
  ClearBoardSelection,
  deviceSelectorFactory,
  driverNameFactory,
  INIT_BOARDS,
  REQ_DRIVER_ASSIGNATION,
  SEND_TO_DRIVER,
  SetBoardSelectedAction,
  SetBoardsListAction,
  SetConfigurationListAction, SetConfigurationsAction,
  SetDriverAssignationAction,
  SetDriversListAction,
  UPDATE_FROM_DRIVER,
  UpdateFromDriverAction
} from './store';

export const registerBoardEpics = (stop) => {
  const deviceSelector = deviceSelectorFactory();
  const driverSelector = driverNameFactory(deviceSelector);
  
  const getDriver = (state, { device }) => driverSelector(state, { device });
  
  registerEpic((action$, state$) => action$.pipe(
    takeUntil(stop),
    ofType(REQ_DRIVER_ASSIGNATION),
    filter(({ driver }) => !!driver),
    withLatestFrom(state$),
    filter(([a, state]) => getDriver(state, a) !== a.driver),
    map(([a, s]) => a),
    map(({ device, driver }) => {
      socket.send({
        type: 'driver-assign',
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
  const process = {
    'drivers': x => of(SetDriversListAction(x.drivers)),
    'devices': x => of(SetBoardsListAction(x.devices)),
    'connectors': x => Object.keys(x.drivers).map(k =>
      SetDriverAssignationAction(k, x.drivers[k])
    ),
    'from-device': x => of(UpdateFromDriverAction(x.deviceId, x.driver, x.data)),
    'configurations': x => of(SetConfigurationListAction(x.configurations)),
    'configurations-per-devices': x => from(Object.keys(x.configs))
    .pipe(
      map(c => SetConfigurationsAction(c, x.configs[c]))
    )
  };
  const keys = Object.keys(process);
  registerEpic(action$ => action$.pipe(
    ofType(INIT_BOARDS),
    mergeMap(() => {
      [
        'drivers-get-state',
        'devices-get',
        'configurations-get',
        'connectors-get-state',
        'configurations-per-devices-get'
      ].map(type => socket.send({ type }));
      
      return socket.messages.pipe(
        filter(x => keys.indexOf(x.type) > -1)
      )
    }),
    mergeMap(x => {
      
      const f = process[x.type];
      if (!f) {
        console.error(`Unknown message type '${x.type}' in ${x}`);
        return EMPTY;
      } else return f(x);
    }),
    takeUntil(stop)
  ));
  
  registerEpic(action$ => {
    const base = action$.pipe(
      takeUntil(stop),
      ofType(UPDATE_FROM_DRIVER),
      filter(({ data }) => data && data.type === 'the-button')
    );
    const clear = base.pipe(
      filter(({ data: { on } }) => !on),
      mapTo(ClearBoardSelection())
    );
    const setSel = base.pipe(
      filter(({ data: { on } }) => !!on),
      map(({ device }) => SetBoardSelectedAction(device))
    );
    
    return merge(clear, setSel);
  })
};

