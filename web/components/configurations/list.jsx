import React from 'react';
import style from './list.scss';
import { connectList } from './store';
import { Link, Switch } from 'react-router-dom';
import Select from 'react-select';
import { Route } from 'react-router';
import socket from '../../utils/socket';
import { BehaviorSubject, Subject } from 'rxjs';
import { Configuration } from './configuration';
import { ConfContext } from './conf-context';

export const Option = ({ label, value, innerRef }) => (
  <div className={style.link} ref={innerRef}>
    <Link to={`/configurations/${!value ? '' : value}`}>{label}</Link>
  </div>
);


class SelectorComponent extends React.Component {
  static contextType = ConfContext;
  state = {
    current: ''
  };
  unmount = new Subject();
  
  componentWillUnmount() {
    this.unmount.next();
  }
  
  componentWillMount() {
    this.context.subscribe(current => this.setState({ current }));
  }
  
  render() {
    const options = [{ label: 'new', value: '' }, ...this.props.names.map(value => ({ value, label: value }))];
    return (
      <div className={style.selector}>
        <Select options={options}
                isMulti={false}
                hideSelectedOptions={true}
                placeholder={'Open configuration'}
                closeMenuOnSelect={true}
                value={options.filter(({ value }) => value === this.state.current)}
                components={{ Option }}/>
      </div>
    )
  }
}

export class ConfigurationsListComponent extends React.PureComponent {
  confContext = new BehaviorSubject(null);
  
  componentWillMount() {
    socket.send({ type: 'configurations-get' });
  }
  
  render() {
    const { names, location } = this.props;
    return (
      <div className={style.container}>
        <ConfContext.Provider value={this.confContext}>
          <SelectorComponent names={names}/>
          <Switch location={location}>
            <Route path={'/configurations/'} exact={true} component={Configuration}/>
            <Route path={'/configurations/:name'} component={Configuration}/>
          </Switch>
        </ConfContext.Provider>
      </div>
    );
  }
}


export const ConfigurationsList = connectList(ConfigurationsListComponent);

