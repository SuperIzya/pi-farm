import React from 'react';
import PropTypes from 'prop-types';
import Client from './client';
import { BehaviorSubject } from 'rxjs';
import {map, pluck} from 'rxjs/operators';

const GlobalLoaderContext = React.createContext();
export const GlobalLoader = ({children}) => (
  <GlobalLoaderContext.Provider value={new BehaviorSubject({})}>
    {children}
  </GlobalLoaderContext.Provider>
);

const promiseLoad = file => new Promise(resolve => {
  Client.get(`get-plugin/${file}`).pipe(
    pluck('data'),
    map(eval),
  ).subscribe(obj => resolve(obj));
});

const LocalLoaderContext = React.createContext();

class Loader extends React.Component {
  static contextType = GlobalLoaderContext;
  state = {
    loaded: false,
    exports: {}
  };
  
  getPromise = bundle => this.context.value[bundle] || promiseLoad(bundle);
  
  componentWillMount() {
    const promise = this.getPromise(this.props.bundle);
    this.context.next({
      ...this.context.value,
      [this.props.bundle]: promise
    });
    promise.then(obj => this.setState({exports: obj, loaded: true}))
  }
  
  render() {
    return !this.state.loaded ? this.props.fallback : (
      <LocalLoaderContext.Provider value={this.state.exports}>
        {this.props.children}
      </LocalLoaderContext.Provider>
    );
  }
}

export class Loaded extends React.PureComponent {
  static contextType = LocalLoaderContext;
  
  render() {
    const {component, ...rest} = this.props;
    const {[component]: Component} = this.context;
    return <Component {...rest}/>;
  }
}

Loader.propTypes = {
  fallback: PropTypes.element,
  bundle: PropTypes.string.isRequired
};

Loaded.propTypes = {
  component: PropTypes.string.isRequired
};

export const loadPlugin = bundle => props => (
  <Loader {...props}
          bundle={bundle} />
);
