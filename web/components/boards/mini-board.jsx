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
import Button from '@material-ui/core/Button';

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
                                                 confNames
                                               }) => {
  const options = confNames.map(k => ({ value: k, label: k }));
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
    <div>{device}</div>
    <div>{driver}</div>
  </div>
);

export const CommonButtons = ({onLedClick, on, onResetClick}) => (
  <div className={style.buttons}>
    <Button className={style.button}
            variant="contained"
            color={on ? "secondary" : "primary"}
            onClick={onLedClick}>
      {on ? 'Led off' : 'Led on'}
    </Button>
    <Button className={style.button}
            variant="contained"
            color={"secondary"}
            onClick={onResetClick}>
      Reset
    </Button>
  </div>
);

export class BoardFooterComponent extends React.PureComponent {
  static contextType = BoardsContext;
  
  render() {
    const { driver, assignDriver, device } = this.props;
    return (
      <div className={style.footer}>
        <DriverSelector driver={driver}
                        assignDriver={assignDriver}
                        drivers={this.context.drivers}/>
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

export class MiniBoardComponent extends React.Component {
  state = {
    on: false
  };
  componentWillMount() {
    this.sendLed(this.state.on);
  
  }
  
  toggleLed = () => this.setState({on: !this.state.on}, () => this.sendLed(this.state.on));
  sendLed = value => this.props.send({ type: 'the-led', value });
  sendReset = () => this.props.send({type: 'reset'});
  
  render() {
    const {device, driver} = this.props;
    return (
      <MiniBoardFrame device={device} driver={driver}>
        <BoardHeader device={device} driver={driver}/>
        <CommonButtons on={this.state.on}
                       onLedClick={this.toggleLed}
                       onResetClick={this.sendReset}/>
        <div className={style.content}>
          <LoadConnected device={device} driver={driver}/>
        </div>
        <BoardFooter driver={driver} device={device}/>
      </MiniBoardFrame>
    );
  }
}

export const MiniBoard = connectMiniBoard(MiniBoardComponent);
