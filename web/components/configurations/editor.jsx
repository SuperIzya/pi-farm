import React from 'react';
import { connectConfiguration } from './store';
import { DropTarget } from 'react-drag-drop-container';
import styles from './editor.scss';
import { DndContext } from './dnd-context';
import { Subject } from 'rxjs';
import { takeUntil, delay } from 'rxjs/operators';
import _ from 'lodash';

const Grid = ({ rows, cols, children }) => (
  <div className={styles.container}
       style={{
         ['--rows']: rows,
         ['--cols']: cols
       }}>
    {children}
  </div>
);

const gridStyle = (row, col) => ({
  ['--row']: row + 1,
  ['--col']: col + 1
});

const EmptyCell = ({ row, col }) => (
  <DropTarget targetKey={'node'}
              dropData={{ row, col }}>
    <div className={styles.cell} style={gridStyle(row, col)}/>
  </DropTarget>
);

const EmptyTarget = ({ onEnter, onLeave }) => (
  <DropTarget targetKey={'node'}
              onDragLeave={onLeave}
              onDragEnter={onEnter}>
  </DropTarget>
);

const Cell = ({ node, row, col, onEnter, onLeave }) => {
  if (!node) return <EmptyCell row={row} col={col}/>;
  
  const props = (r, c) => ({
    onEnter: () => onEnter(r, c),
    onLeave: () => onLeave(r, c)
  });
  const diff = [-1, 0, 1];
  return (
    <div className={styles.cell} style={gridStyle(row, col)}>
      {
        diff.flatMap((r, i) => diff.map((c, j) => {
          if (!r && !c) return null;
          return <EmptyTarget {...props(row + r, col + c)} key={`${i}-${j}`}/>;
        }))
      }
      <div className={styles.node}>{node.name || 'No name'}</div>
    </div>
  );
};

const isNew = (curr, max) => curr < 0 || curr >= max;

class EditorComponent extends React.Component {
  static contextType = DndContext;
  
  unmount = new Subject();
  
  state = {
    rows: 1,
    cols: 1,
    nodes: {}
  };
  
  componentWillUnmount() {
    this.unmount.next();
  }
  
  componentWillMount() {
    this.context.dropped.pipe(
      takeUntil(this.unmount),
      delay(1)
    ).subscribe(({ dragData, dropData: { row, col } }) => {
      const { nodes, rows, cols } = this.state;
      const newState = {
        rows: isNew(row, rows) ? rows + 1 : rows,
        cols: isNew(col, cols) ? cols + 1 : cols,
        nodes: {
          ...nodes,
          [col]: {
            ...(nodes[col] || {}),
            [row]: dragData
          }
        }
      };
      this.setState(newState);
    })
  }
  
  getNode = (r, c) => (this.state.nodes[c] || {})[r] || null;
  
  onEnter = (row, col) => {
    const { nodes, rows, cols } = this.state;
    
    const newRow = isNew(row, rows);
    const newCol = isNew(col, cols);
    
    const column = !newCol ? nodes[col] : {};
    const cell = !newRow ? column[row] || null : null;
    let ns = nodes;
    if (col < 0) {
      ns = Object.keys(nodes)
      .map(parseInt)
      .map(k => ({ [k + 1]: nodes[k] }))
      .reduce((a, b) => ({ ...a, ...b }), {});
    }
    const newNodes = {
      ...ns,
      [Math.max(0, col)]: {
        ...column,
        [row]: cell
      }
    };
    
    this.setState({
        nodes: newNodes,
        cols: newCol ? cols + 1 : cols,
        rows: newRow ? rows + 1 : rows
      }
    );
  };
  
  onLeave = (row, col) => {
    const { nodes, rows, cols } = this.state;
    const c = Math.max(0, col);
    const column = nodes[c] || {};
    const { [row]: lost, ...newColumn } = column;
    if (!newColumn || _.isEmpty(newColumn) || newColumn.every(x => !x)) {
      const { [c]: lost2, ...newNodes } = nodes;
      this.setState({ nodes: newNodes, cols: cols - 1 });
    } else if (newColumn) {
      this.setState({
        nodes: {
          ...nodes,
          [col]: newColumn,
        }
      })
    } else {
      const { [c]: lost2, ...newNodes } = nodes;
      this.setState({ nodes: newNodes });
    }
    
  };
  
  render() {
    return (
      <Grid cols={this.state.cols}
            rows={this.state.rows}
      >
        {
          new Array(this.state.cols).fill(null)
          .flatMap((x, col) => new Array(this.state.rows).fill(null)
            .map((c, row) => (
              <Cell node={this.getNode(row, col)}
                    row={row}
                    col={col}
                    onEnter={this.onEnter}
                    onLeave={this.onLeave}
                    key={`${row}-${col}`}/>
            ))
          )
        }
      </Grid>
    )
  }
}

export const Editor = connectConfiguration(EditorComponent);



