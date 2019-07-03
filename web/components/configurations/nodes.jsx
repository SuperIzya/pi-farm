import React from 'react';
import styles from './nodes.scss';
import { connectNode, connectNodesList } from './store';
import { Subject } from 'rxjs';
import socket from '../../utils/socket';
import { takeUntil, map } from 'rxjs/operators';
import { ofType } from 'redux-observable';
import { DragDropContainer } from 'react-drag-drop-container';
import classNames from 'classnames';
import { DndContext } from './dnd-context';

class NodeComponent extends React.Component {
  static contextType = DndContext;
  
  state = {
    isDrag: false
  };
  
  startDrag = () => this.setState({ isDrag: true });
  stopDrag = () => this.setState({ isDrag: false });
  
  render() {
    const { node } = this.props;
    const { name } = node;
    
    return (
      <div className={styles.dragContainer}>
        <DragDropContainer targetKey={'node'}
                           dragClone={true}
                           onDrop={d => this.context.dropped.next(d)}
                           onDragStart={this.startDrag}
                           onDragEnd={this.stopDrag}
                           dragData={node}>
          <div className={classNames(styles.node, { [styles.drag]: this.state.isDrag })}>
            {name}
          </div>
        </DragDropContainer>
      </div>
    )
  }
}

const Node = connectNode(NodeComponent);

class NodesComponent extends React.PureComponent {
  unmount = new Subject();
  
  componentWillUnmount() {
    this.unmount.next();
  }
  
  componentWillMount() {
    socket.send({ type: 'configuration-nodes-get' });
    socket.messages.pipe(
      takeUntil(this.unmount),
      ofType('configuration-nodes'),
      map(x => x.nodes)
    ).subscribe(d => this.props.onData(d))
  }
  
  render() {
    const { count } = this.props;
    return (
      <div className={styles.container}>
        {new Array(count).fill(0).map((x, i) => <Node index={i} key={i}/>)}
      </div>
    );
  }
}

export const Nodes = connectNodesList(NodesComponent);
