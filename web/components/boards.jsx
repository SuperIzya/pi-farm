import React from 'react';

import { connectToBoards } from './boards.js';
import Loading from '../icons/loading';
import styles from './boards.scss';

const Boards = ({ boards, initBoards }) => {
  if (!boards || !boards.length) {
    initBoards();
    return <Loading/>;
  }
  
  return (
    <div className={styles.container}>
      {boards.map((b, k) => <span key={k}>{b}</span>)}
    </div>
  );
};

export default connectToBoards(Boards);
