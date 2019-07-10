import React from 'react';
import { ConfContext } from './conf-context';
import _ from 'lodash';
import { NodeDetail } from './node-detail';
import styles from './editor.scss';
import GGEditor, {
  Flow,
  DetailPanel,
  NodePanel,
  EdgePanel,
  GroupPanel,
  MultiPanel,
  CanvasPanel,
  Minimap
} from 'gg-editor';
import { EditorToolbar } from './editor-toolbar';
import { Nodes } from './nodes';
import { RegisterNodeTypes } from './register-nodes';


export class Editor extends React.Component {
  static contextType = ConfContext;
  state = {
    show: false
  };
  
  procProps = ({ match: { params: { name } } }) => this.context.next(name || '');
  
  componentWillMount() {
    this.procProps(this.props);
    clearTimeout(this.timeout);
  }
  
  componentDidMount() {
    this.timeout = setTimeout(() => this.setState({ show: true }), 100);
  }
  
  componentWillUnmount() {
    this.procProps({ match: { params: { name: '' } } });
  }
  
  componentWillReceiveProps(nextProps, nextContext) {
    if (!_.isEqual(this.props, nextProps))
      this.procProps(nextProps);
  }
  
  render() {
    return (
      <GGEditor className={styles.container}>
        <RegisterNodeTypes/>
        <div className={styles.toolbar}>
          <EditorToolbar/>
        </div>
        <div className={styles.flow}>
          {!this.state.show ? null : <Flow className={styles.graph}/>}
        </div>
        <div className={styles.nodes}>
          <Nodes/>
        </div>
        <div className={styles.details}>
          
          <NodeDetail className={styles.form}/>
          
          <Minimap height={200} className={styles.map}/>
        </div>
      </GGEditor>
    );
  }
  
}



