import React from 'react';
import styles from './board.scss';
import { connectBoard } from './board.js';
import socket from '../utils/socket';

const Board = ({ board, toggleFilter, isOn }) => {
  const status = isOn ? 'Off' : 'On';
  
  const trigger = () => {
    toggleFilter();
    socket.send(`[${board}]cmd: blink`);
  };
  
  return (
    <div className={`${styles.container} ${styles[status]}`}>
      <button className={styles.button} onClick={trigger}>
        {board}
      </button>
    </div>
  )
};

export default connectBoard(Board);

