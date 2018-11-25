import React from 'react';
import styles from './board.scss';
import { connectBoard } from './board.js';

const Board = ({ board, triggerFilter, isOn }) => {
  const status = isOn ? 'Off' : 'On';
  
  return (
  <div className={`${styles.container} ${styles[status]}`}>
    <button className={styles.button} onClick={triggerFilter}>
      {board}
    </button>
  </div>
  )
};

export default connectBoard(Board);

