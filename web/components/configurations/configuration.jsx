import React from 'react';
import { ConfContext } from './conf-context';
import _ from 'lodash';
import { connectConfiguration } from './store';

export class ConfigurationComponent extends React.PureComponent {
  static contextType = ConfContext;
  
  procProps = ({match: {params: {name}}}) => this.context.next(name);
  componentWillMount() {
    this.procProps(this.props);
  }
  componentWillUnmount() {
    this.procProps({match: {params: {name: ''}}});
  }
  componentWillReceiveProps(nextProps, nextContext) {
    if(!_.isEqual(this.props, nextProps))
      this.procProps(nextProps);
  }
  
  render() {
    const {configuration} = this.props;
    return JSON.stringify(configuration) || null;
  }
  
}


export const Configuration = connectConfiguration(ConfigurationComponent);

