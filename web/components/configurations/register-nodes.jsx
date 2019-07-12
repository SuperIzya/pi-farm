import React from 'react';
import { RegisterNode } from 'gg-editor';
import { connectNode, connectNodesList } from './store';

const RegisterNodeComponent = ({ node: { name, anchors } }) => {
  return (
    <RegisterNode name={name}
                  extend={'flow-rect'}
                  config={{
                    anchor: anchors
                  }}
    />
  );
};

const RegNode = connectNode(RegisterNodeComponent);

const RegisterNodes = ({ count }) => (
  <React.Fragment>
    {new Array(count).fill(0).map((x, i) => <RegNode index={i} key={i}/>)}
  </React.Fragment>
);

export const RegisterNodeTypes = connectNodesList(RegisterNodes);
