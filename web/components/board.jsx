import React from 'react';
import styles from './board.scss';

const FilterStatus = ({filter}) => (
  <div className={styles.filter}>
    {filter ? 'Off' : 'On'}
  </div>
);


const Board = ({board, triggerLogFilter}) => (
  <div className={styles.container}>
    <button className={styles.button} onClick={triggerLogFilter}>
      <FilterStatus filter={board.filter}/>
    </button>
  </div>
);

export default Board;

