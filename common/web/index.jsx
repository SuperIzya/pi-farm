import React from 'react';
import * as defaultDriver from './default-driver';

const res = {
  ...defaultDriver
};

const Library = props => {
  const {component, ...rest} = props;
  const C = res[component];
  
  return C ? <C {...rest}/> : null;
};

export default Library;
