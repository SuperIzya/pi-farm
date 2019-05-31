import React from 'react';
import style from './mini-board.scss';
import { Button } from '@material-ui/core';
import { Dial, Hand } from '../common/sensor-clock';
import _ from 'lodash';
import { LeftIcon, RightIcon, StopIcon } from './icons';


const Wrapper = ({ onClick, on, children }) => (
  <div className={style.container}>
    <Button className={style.button}
            variant="contained"
            color={on ? "secondary" : "primary"}
            onClick={onClick}>
      {on ? 'Led off' : 'Led on'}
    </Button>
    {children}
  </div>
);


const TempHand = ({ data }) => <Hand value={(data.temp || {}).value || 0}/>;
const HumidHand = ({ data }) => <Hand value={(data.humid || {}).value || 0}/>;

export class MiniBoard extends React.Component {
  state = {
    on: false
  };
  
  sendLed = value => this.props.send({ type: 'the-led', value });
  sendDir = spin => this.props.send({ type: 'the-spin', spin });
  
  componentWillMount() {
    this.sendLed(this.state.on);
    this.hands = {
      Temp: this.props.connectData(TempHand),
      Humid: this.props.connectData(HumidHand)
    };
  }
  
  shouldComponentUpdate(nextProps, nextState, nextContext) {
    return _.isEqual(nextProps, this.props) && !_.isEqual(this.state, nextState);
  }
  
  componentWillReceiveProps(nextProps, nextContext) {
    this.procProps(nextProps);
  }
  
  onClick = () => this.setState({ on: !this.state.on }, () => this.sendLed(this.state.on));
  
  render() {
    const { Temp, Humid } = this.hands;
    return (
      <Wrapper onClick={this.onClick} on={this.state.on}>
        <div className={style.dials}>
          <div className={style.dial}>
            <Dial to={100} from={0} step={5} numbersToPrint={n => !(n % 10)}>
              <Humid driver={this.props.driver} device={this.props.device}/>
            </Dial>
          </div>
          <div className={style.dial}>
            <Dial to={80} from={-40} step={12} numbersToPrint={false}>
              <Temp driver={this.props.driver} device={this.props.device}/>
            </Dial>
          </div>
        </div>
        <div className={style.directions}>
          <Button className={style.button}
                  variant={"contained"}
                  color={"default"}
                  onClick={() => this.sendDir(-1)}>
            <LeftIcon/>
          </Button>
          <Button className={style.button}
                  variant={"contained"}
                  color={"default"}
                  onClick={() => this.sendDir(0)}>
            <StopIcon/>
          </Button>
          <Button className={style.button}
                  variant={"contained"}
                  color={"default"}
                  onClick={() => this.sendDir(1)}>
            <RightIcon/>
          </Button>
        </div>
      </Wrapper>
    );
  }
}
