import React from 'react';

import { connectToBoards, registerBoardEpics } from './store.js';
import Loading from '../../icons/loading';
import styles from './boards.scss';
import { Subject } from 'rxjs';
import { MiniBoard } from './mini-board';

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
        {boardNames.map(b => <MiniBoard key={b} name={b}/>)}
      </div>
    );
  };
}

export default connectToBoards(Boards);
