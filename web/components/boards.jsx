import React from 'react';

import { connectToBoards, registerBoardEpics } from './boards.js';
import Loading from '../icons/loading';
import styles from './boards.scss';
import Board from './board';
import { Subject } from 'rxjs';

class Boards extends React.PureComponent {
  unmount = new Subject();
  
  constructor(props) {
    super(props);
    registerBoardEpics(this.unmount);
    props.initBoards();
  }
  
  componentWillUnmount() {
    this.unmount.next();
    this.unmount.complete();
  }
  
  render() {
    const { boardNames } = this.props;
    if (!boardNames || !boardNames.length) {
      
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
}

export default connectToBoards(Boards);
