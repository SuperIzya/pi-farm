import React from 'react';
import styles from './edge-details.scss';
import { withPropsAPI } from "gg-editor";
import { Card, Description, Line } from './card';
import { EllipsisedText } from './ellipsised-text';


const EdgeErrors = ({ errors }) => !errors || !errors.length ? null : (
  <div className={styles.errors}>
    {errors.map((e, i) => <div className={styles.error} key={i}>{e}</div>)}
  </div>
);
const EdgeConnection = ({ connection, name }) => (
  <Line name={name}>
    <div className={styles.connName}>
      <EllipsisedText text={connection.name}/>
      <div className={styles.unit}>
        (<EllipsisedText text={connection.unit}/>)
      </div>
    </div>
  </Line>
);
export const EdgeDetails = withPropsAPI(({ propsAPI }) => {
  const [{ model: { errors, source, target, sourceAnchor, targetAnchor } }, ...x] = propsAPI.getSelected();
  const src = propsAPI.find(source).model;
  const dst = propsAPI.find(target).model;
  const sourceConnection = src.node.connections[sourceAnchor];
  const targetConnection = dst.node.connections[targetAnchor];
  return (
    <Card title={'Edge'}>
      <Description>
        <EdgeConnection name={'source'} connection={sourceConnection}/>
        <EdgeConnection name={'target'} connection={targetConnection}/>
      </Description>
      <EdgeErrors errors={errors}
                  src={sourceConnection}
                  dst={targetConnection}/>
    </Card>
  );
});

