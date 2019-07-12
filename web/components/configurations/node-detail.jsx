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
import { Card, Description, Line } from './card';
import classNames from 'classnames';

const Connection = ({ x: { name, unit } }) => (
  <React.Fragment>
    <div className={styles.name}>{name}</div>
    <div className={styles.unit}>{unit}</div>
  </React.Fragment>
);

const Connections = ({ name, connections }) => !connections.length ? null : (
  <Line name={name}>
    <div className={styles.connections}>
      {connections.map((x, i) => <Connection x={x} key={i}/>)}
    </div>
  </Line>
);


const DNode = withPropsAPI(({ propsAPI }) => {
  const [{ model: { node: { inputs, outputs, name, type } } }, ...x] = propsAPI.getSelected();
  return (
    <Card title={'Node'}>
      <Description>
        <Line name={'name'} value={name}/>
        <Line name={'type'} value={type}/>
        <Connections connections={inputs} name={'inputs'}/>
        <Connections connections={outputs} name={'outputs'}/>
      </Description>
    </Card>
  );
});

const DEdge = withPropsAPI(props => {
  debugger;
  return <Card title={'Edge'}/>;
});

const DGroup = withPropsAPI(props => {
  debugger;
  return <Card title={'Group'}/>;
});

const Status = () => <Card title={'Status'}/>;

export const NodeDetail = ({ className }) => (
  <DetailPanel className={classNames(styles.container, className)}>
    <NodePanel className={styles.panel}>
      <DNode/>
    </NodePanel>
    <EdgePanel className={styles.panel}>
      <DEdge/>
    </EdgePanel>
    <GroupPanel className={styles.panel}>
      <DGroup/>
    </GroupPanel>
    <MultiPanel className={styles.panel}>
      Empty
    </MultiPanel>
    <CanvasPanel className={styles.panel}>
      <Status/>
    </CanvasPanel>
  </DetailPanel>
);
