import React from 'react';
import style from './mini-board.scss';
import { Button } from '@material-ui/core';
import { LeftIcon, RightIcon, StopIcon } from './icons';

export class MiniMotorBoard extends React.Component {
  state = {
    on: false
  };
  
  sendLed = value => this.props.send({ type: 'the-led', value });
  
  sendDir = value => this.props.send({type: 'direction', value});
  
  componentDidMount() {
    this.send(this.state.on);
  }
  
  onClick = () => this.setState({ on: !this.state.on }, () => this.sendLed(this.state.on));
  
  render() {
    const { device, driver } = this.props;
    return (
      <div className={style.container}>
        <div className={style.buttonContainer}>
          <Button className={style.button}
                  variant="contained"
                  color={this.state.on ? "secondary" : "primary"}
                  onClick={this.onClick}>
            {this.state.on ? 'Led off' : 'Led on'}
          </Button>
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
        </div>
        <span className={style.text}>{device} Mini board {driver}</span>
      </div>
    );
  }
}
