import React from 'react';
import { ConfContext } from './conf-context';
import _ from 'lodash';
import { Layout, Toolbar, Workspace } from './layout';
import { Nodes } from './nodes';
import { Editor } from './editor';

export class Configuration extends React.PureComponent {
  static contextType = ConfContext;
  
  procProps = ({ match: { params: { name } } }) => this.context.next(name || '');
  
  componentWillMount() {
    this.procProps(this.props);
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
      <Layout>
        <Toolbar>
          <Nodes/>
        </Toolbar>
        <Workspace>
          <Editor/>
        </Workspace>
      </Layout>
    );
  }
  
}



