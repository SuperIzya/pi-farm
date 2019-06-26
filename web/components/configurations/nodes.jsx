import React from 'react';
import { useDrag } from 'react-dnd';
import styles from './nodes.scss';
import classNames from 'classnames';
import { connectNode, connectNodesList } from './store';
import { Subject } from 'rxjs';
import socket from '../../utils/socket';
import { takeUntil, map } from 'rxjs/operators';
import { ofType } from 'redux-observable';

const NodeComponent = ({node: {name, type}}) => {
  if(!name) return null;
  const [{ isDragging }, drag] = useDrag({
    item: {name, type: 'node'},
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  });
  
  return (
    <div className={classNames(styles.node, {[styles.drag]: isDragging})} ref={drag}>
      {name}
    </div>
  )
};

const Node = connectNode(NodeComponent);

class NodesComponent extends React.PureComponent {
  unmount = new Subject();
  
  componentWillUnmount() {
    this.unmount.next();
  }
  
  componentWillMount() {
    socket.send({type: 'configuration-nodes-get'});
    socket.messages.pipe(
      takeUntil(this.unmount),
      ofType('configuration-nodes'),
      map(x => x.nodes)
    ).subscribe(d => this.props.onData(d))
  }
  
  render() {
    const {count} = this.props;
    return (
      <div className={styles.container}>
        {new Array(count).fill(0).map((x, i) => <Node index={i} key={i}/>)}
      </div>
    );
  }
}

export const Nodes = connectNodesList(NodesComponent);
