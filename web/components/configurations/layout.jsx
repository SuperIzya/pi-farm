import React from 'react';
import styles from './layout.scss';
import { DragIcon } from '../../icons';
import { BehaviorSubject, fromEvent, Subject } from 'rxjs';
import { takeUntil, filter, withLatestFrom } from 'rxjs/operators';

export class Layout extends React.Component {
  state = {
    toolbar: 200
  };
  
  ref = React.createRef();
  unmount = new Subject();
  track = new BehaviorSubject(-1);
  componentWillUnmount() {
    this.unmount.next();
  }
  componentDidMount() {
    const {current} = this.ref;
    
    fromEvent(current, 'mousemove').pipe(
      takeUntil(this.unmount),
      withLatestFrom(this.track),
      filter(([e, t]) => t >= 0),
    ).subscribe(([e, t]) => this.setState({
        toolbar: e.clientX - t
      }));
    
  }
  
  start = e => this.track.next(e.clientX - this.state.toolbar);
  
  stop = () => this.track.next(-1);
  render() {
    
    const style = {'--toolbar': `${this.state.toolbar}px`};
    
    return (
      <div className={styles.container}
           style={style}
           ref={this.ref}
           onMouseLeave={this.stop}
           onMouseUp={this.stop}>
        {this.props.children}
        <Divider onDragStart={this.start}/>
      </div>
    )
  }
}

export const Workspace = ({ children }) => <div className={styles.workspace}>{children}</div>;

export const Toolbar = ({ children }) => <div className={styles.toolbar}>{children}</div>;

export const Divider = ({onDragStart}) => (
  <div className={styles.div}>
    <div className={styles.drag} onMouseDown={onDragStart}>
      <DragIcon/>
    </div>
  </div>
);

