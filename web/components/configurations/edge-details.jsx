import React from 'react';
import styles from './edge-details.scss';
import { withPropsAPI } from "gg-editor";
import { Card } from './card';



const EdgeErrors = ({ errors }) => !errors || !errors.length ? null : (
  <div className={styles.errors}>
    {errors.map((e, i) => <div className={styles.error} key={i}>{e}</div>)}
  </div>
);
const EdgeConnection = ({ connection, name }) => (
  <div className={styles.connDescription}>
    <div className={styles.name}>{name}</div>
    <div className={styles.connName}>
      <div className={styles.name}>{connection.name}</div>
      <div className={styles.unit}>(<div className={styles.name}>{connection.unit}</div>)</div>
    </div>
  </div>
);
export const EdgeDetails = withPropsAPI(({ propsAPI }) => {
  const [{ model: { errors, source, target, sourceAnchor, targetAnchor } }, ...x] = propsAPI.getSelected();
  const src = propsAPI.find(source).model;
  const dst = propsAPI.find(target).model;
  const sourceConnection = src.node.connections[sourceAnchor];
  const targetConnection = dst.node.connections[targetAnchor];
  return (
    <Card title={'Edge'}>
      <div className={styles.edgeConnections}>
        <EdgeConnection name={'source'} connection={sourceConnection}/>
        <EdgeConnection name={'target'} connection={targetConnection}/>
      </div>
      <EdgeErrors errors={errors}
                  src={sourceConnection}
                  dst={targetConnection}/>
    </Card>
  );
});

