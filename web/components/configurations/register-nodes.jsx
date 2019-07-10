import React from 'react';
import { RegisterNode } from 'gg-editor';
import { connectNode, connectNodesList } from './store';

const RegisterNodeComponent = ({ node: { name, inputs, outputs } }) => {
  const ins = inputs || [];
  const outs = outputs || [];
  const inLen = ins.length;
  const outLen = outs.length;
  return (
    <RegisterNode name={name}
                  extend={'flow-rect'}
                  config={{
                    anchor: [
                      ...ins.map((x, i) => [(i + 1) / (inLen + 1), 0]),
                      ...outs.map((x, i) => [(i + 1) / (outLen + 1), 1])
                    ]
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
