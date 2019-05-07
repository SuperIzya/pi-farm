import React from 'react';
import { connectBoard } from './store.js';
import Loading from '../../icons/loading';
import { Loaded, loadPlugin } from '../../utils/loader';
import style from './mini-board.scss';
import {BoardsContext} from './context';
import Select from 'react-select';

const LoadComponent = ({meta, device, driver, send, data}) => {
  const Loader = loadPlugin(meta.index);
  const loading = <Loading/>;
  
  return !(meta && meta.mini) ? null : (
    <Loader fallback={loading}>
      <Loaded component={meta.mini}
              device={device}
              data={data}
              driver={driver}
              send={send}/>
    </Loader>
  );
};

export const DriverSelector = ({driver, drivers, assignDriver}) => {
  const options = drivers.map((d, i) => ({value: i, label: d.name}));
  const value = options.find(o => o.label === driver);
  return (
    <div className={style.selector}>
      <Select options={options}
              onChange={assignDriver}
              value={value}/>
    </div>
  );
};

export class MiniBoardComponent extends React.PureComponent {
  static contextType = BoardsContext;
  
  render() {
    const { device, driver, assignDriver } = this.props;
    return (
      <div className={style.container}>
        <div className={style.header}>
          <div className={style.label}>Device</div>
          <div className={style.text}>{device}</div>
          <div className={style.label}>Driver</div>
          <div className={style.text}>{driver}</div>
        </div>
        <LoadComponent {...this.props}/>
        <div className={style.footer}>
          <div>Change driver</div>
          <DriverSelector driver={driver}
                          assignDriver={assignDriver}
                          drivers={this.context.drivers}/>
        </div>
      </div>
    );
  }
}

export const MiniBoard = connectBoard(MiniBoardComponent);
