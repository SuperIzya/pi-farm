import React from 'react';
import { connectConfigurationSelector } from './store.js';
import Loading from '../../icons/loading';
import { Loaded, loadPlugin } from '../../utils/loader';
import style from './mini-board.scss';
import { BoardsContext } from './context';
import Select from 'react-select';
import classNames from 'classnames';
import {
  connectBoardData,
  connectBoardFooter,
  connectBoardFrame,
  connectInnerBoard,
  connectMiniBoard
} from './store';

const LoadComponent = ({ index, mini, device, driver, send }) => {
  const Loader = loadPlugin(index);
  const loading = <Loading/>;
  
  return !mini ? null : (
    <Loader fallback={loading}>
      <Loaded component={mini}
              device={device}
              bundle={index}
              connectData={connectBoardData}
              driver={driver}
              send={send}/>
    </Loader>
  );
};

const selectStyles = {
  menu: (o1, o2) => ({
    ...o1,
    marginTop: 0,
    marginBottom: 0
  })
};

export const DriverSelector = ({ driver, drivers, assignDriver }) => {
  const options = drivers.map((d, i) => ({ value: i, label: d.name }));
  const value = options.find(o => o.label === driver);
  return (
    <div className={style.selector}>
      <Select options={options}
              menuPlacement={'auto'}
              onChange={assignDriver}
              styles={selectStyles}
              value={value}/>
    </div>
  );
};

export const ConfigurationSelectorComponent = ({
                                                 driver,
                                                 configurations,
                                                 selectConfigs,
                                                 allConfigurations
                                               }) => {
  const options = Object.keys(allConfigurations).map(k => ({ value: k, label: k }));
  const values = options.filter(o => configurations.indexOf(o.label) > -1);
  return (
    <div className={style.selector}>
      <Select options={options}
              isMulti={true}
              isSearchable={true}
              menuPlacement={'auto'}
              isDisabled={!driver}
              styles={selectStyles}
              onChange={selectConfigs}
              value={values}/>
    </div>
  );
};

export const ConfigurationSelector = connectConfigurationSelector(ConfigurationSelectorComponent);

export const BoardHeader = ({ device, driver }) => (
  <div className={style.header}>
    <div className={style.label}>Device</div>
    <div className={style.text}>{device}</div>
    <div className={style.label}>Driver</div>
    <div className={style.text}>{driver}</div>
  </div>
);

export class BoardFooterComponent extends React.PureComponent {
  static contextType = BoardsContext;
  
  render() {
    const { driver, assignDriver, device } = this.props;
    return (
      <div className={style.footer}>
        <div>Change driver</div>
        <DriverSelector driver={driver}
                        assignDriver={assignDriver}
                        drivers={this.context.drivers}/>
        <div>Change configuration</div>
        <ConfigurationSelector driver={driver} device={device}/>
      </div>
    )
  }
}

export const BoardFooter = connectBoardFooter(BoardFooterComponent);

export const MiniBoardFrameComponent = ({ selected, children }) => (
  <div className={classNames(style.container, { [style.selected]: selected })}>
    {children}
  </div>
);

export const MiniBoardFrame = connectBoardFrame(MiniBoardFrameComponent);

const LoadConnected = connectInnerBoard(LoadComponent);

export const MiniBoardComponent = ({device, driver}) => (
  <MiniBoardFrame device={device} driver={driver}>
    <BoardHeader device={device} driver={driver}/>
    <div className={style.content}>
      <LoadConnected device={device} driver={driver}/>
    </div>
    <BoardFooter driver={driver} device={device}/>
  </MiniBoardFrame>
);

export const MiniBoard = connectMiniBoard(MiniBoardComponent);