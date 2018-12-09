import React from 'react';

import { connectToBoards } from './boards.js';
import Loading from '../icons/loading';
import styles from './boards.scss';
import Board from './board';

const Boards = ({ boardNames, initBoards }) => {
  if (!boardNames || !boardNames.length) {
    initBoards();
    return <Loading/>;
  }
  
  const style = {
    gridTemplateColumns: `repeat(${boardNames.length}, auto)`
  };
  
  return (
    <div className={styles.container} style={style}>
      {boardNames.map((b, k) => <Board key={k} board={b}/>)}
    </div>
  );
};

export default connectToBoards(Boards);
