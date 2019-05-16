import React from 'react';
import style from './mini-board.scss';
import { Button } from '@material-ui/core';

export class MiniBoard extends React.Component {
  state = {
    on: false
  };
  
  send = value => this.props.send({ type: 'the-led', value });
  
  componentDidMount() {
    this.send(this.state.on);
  }
  
  onClick = () => this.setState({ on: !this.state.on }, () => this.send(this.state.on));
  
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
        </div>
        <span className={style.text}>{device} Mini board {driver}</span>
      </div>
    );
  }
}
