import React from 'react';
import styles from './board.scss';
import { connectBoard, connectHand } from './board.js';
import socket from '../utils/socket';
import Button from '@material-ui/core/Button';
import { Commands } from '../utils/commands';
import {Dial, Hand } from './sensor-clock';

const send = cmd => () => socket.send(cmd);
const blink = board => send(Commands.blink(board));
const toggle = board => send(Commands.toggle(board));
const reset = board => send(Commands.reset(board));

const ClockHand = connectHand(Hand);

const Board = ({ board, toggleFilter, isOn, ledState }) => {
  const isOrIsnt = (is, isnt) => isOn ? is : isnt;
  const color = ledState > 0 ? 'secondary': 'primary';
  
  return (
    <div className={`${styles.container} ${styles[isOrIsnt('On', 'Off')]}`}>
      <div className={styles.board}>{board}</div>
      <div className={styles.dials}>
        <Dial from={0} to={100} step={5} numbersToPrint={i => !(i % 20)}>
          <ClockHand board={board}
                     sensor={'temperature'}
                     style={`${styles.hand} ${styles.temperature}`}/>
          <ClockHand board={board}
                     sensor={'humidity'}
                     style={`${styles.hand} ${styles.humidity}`}/>
        </Dial>
        <Dial from={-100} to={100} step={10} numbersToPrint={i => !(i % 50)}>
          <ClockHand board={board}
                     sensor={'moisture'}
                     style={`${styles.hand} ${styles.moisture}`}/>
        </Dial>
      </div>
      <div className={styles.buttons}>
        <Button className={styles.button}
                variant="contained"
                color={color}
                onClick={toggle(board)}>
          Toggle
        </Button>
        <Button className={styles.button}
                variant="contained"
                color="primary"
                onClick={blink(board)}>
          Blink
        </Button>
  
        <Button className={styles.button}
                variant="contained"
                color="primary"
                onClick={reset(board)}>
          Reset
        </Button>
        <Button className={`${styles.button} ${styles.log}`}
                variant="contained"
                color={isOrIsnt('primary', 'secondary')}
                onClick={toggleFilter}>
          {isOrIsnt('Off logs','On logs')}
        </Button>
      </div>
    </div>
  )
};

export default connectBoard(Board);

