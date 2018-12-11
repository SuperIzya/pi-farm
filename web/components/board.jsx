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

const ClockHand = connectHand(Hand);

const Board = ({ board, toggleFilter, isOn }) => {
  const isOrIsnt = (is, isnt) => isOn ? is : isnt;
  
  return (
    <div className={`${styles.container} ${styles[isOrIsnt('On', 'Off')]}`}>
      <div className={styles.board}>{board}</div>
      <div className={styles.dials}>
        <Dial to={80} from={10} step={5} numbersToPrint={i => !(i % 10)}>
          <ClockHand board={board} sensor={'s1'}/>
        </Dial>
        <Dial to={120} from={30} step={5} numbersToPrint={i => !(i % 30)}>
          <ClockHand board={board} sensor={'s2'}/>
        </Dial>
      </div>
      <div className={styles.buttons}>
        <Button className={styles.button}
                variant="contained"
                color="primary"
                onClick={toggle(board)}>
          Toggle
        </Button>
        <Button className={styles.button}
                variant="contained"
                color="primary"
                onClick={blink(board)}>
          Blink
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

