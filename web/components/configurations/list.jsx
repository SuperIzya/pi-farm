import React from 'react';
import style from './list.scss';
import { connectList } from './store';
import { Link, Switch } from 'react-router-dom';
import Select from 'react-select';
import { Route } from 'react-router';
import socket from '../../utils/socket';
import classNames from 'classnames';

export const Option = ({ label, innerRef, isFocused }) => (
  <div className={classNames(style.link, { [style.focused]: isFocused })}>
    <Link innerRef={innerRef} to={`/configurations/${label}`}>{label}</Link>
  </div>
);

export class ConfigurationsListComponent extends React.PureComponent {
  componentWillMount() {
    socket.send({ type: 'configurations-get' });
  }
  
  render() {
    const { names, location } = this.props;
    const options = names.map(value => ({ value, label: value }));
    return (
      <div className={style.container}>
        <Select options={options}
        
                isMulti={false}
                components={{ Option }}/>
        
        <Switch location={location}>
          <Route path={'/configurations/:name'}>
            Hello
          </Route>
        </Switch>
      </div>
    );
  }
}


export const ConfigurationsList = connectList(ConfigurationsListComponent);

