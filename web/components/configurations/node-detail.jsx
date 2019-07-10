import React from 'react';
import styles from './node-detail.scss';
import {
  DetailPanel,
  NodePanel,
  EdgePanel,
  GroupPanel,
  MultiPanel,
  CanvasPanel,
  withPropsAPI
} from 'gg-editor';
import { Card } from './card';
import classNames from 'classnames';

const DNode = withPropsAPI(({propsAPI}) => {
  debugger;
  return <Card title={'Node'}/>;
});

const DEdge = withPropsAPI(props => {
  debugger;
  return <Card title={'Edge'}/>;
});

const DGroup = withPropsAPI(props => {
  debugger;
  return <Card title={'Group'}/>;
});

export const NodeDetail = ({className}) => (
  <DetailPanel className={classNames(styles.container, className)}>
    <NodePanel>
      <DNode/>
    </NodePanel>
    <EdgePanel>
      <DEdge/>
    </EdgePanel>
    <GroupPanel>
      <DGroup/>
    </GroupPanel>
    <MultiPanel>
      Empty
    </MultiPanel>
    <CanvasPanel>
      Empty
    </CanvasPanel>
  </DetailPanel>
);
