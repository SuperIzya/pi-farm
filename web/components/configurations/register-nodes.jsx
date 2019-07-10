import React from 'react';
import {RegisterNode} from 'gg-editor';
import { connectNode, connectNodesList } from './store';

const RegisterNodeComponent = ({node: {name}}) => (
  <RegisterNode name={name}
                extend={'flow-capsule'}
                config={{}}
                />
);

const RegNode = connectNode(RegisterNodeComponent);

const RegisterNodes = ({count}) => (
  <React.Fragment>
    {new Array(count).fill(0).map((x, i) => <RegNode index={i} key={i}/>)}
  </React.Fragment>
);

export const RegisterNodeTypes = connectNodesList(RegisterNodes);
