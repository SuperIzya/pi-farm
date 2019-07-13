import React from 'react';
import styles from './edge-details.scss';
import { withPropsAPI } from "gg-editor";
import { Card, Description, Line } from './card';
import { EllipsisedText } from './ellipsised-text';
import Select from 'react-select';


const EdgeErrors = ({ errors }) => !errors || !errors.length ? null : (
  <div className={styles.errors}>
    {errors.map((e, i) => <div className={styles.error} key={i}>{e}</div>)}
  </div>
);


const Single = ({data: {name, unit, value}, selectOption}) => {
  return (
    <div className={styles.connName} onClick={() => selectOption && selectOption(value)}>
      <EllipsisedText text={name}/>
      <div className={styles.unit}>
        (<EllipsisedText text={unit}/>)
      </div>
    </div>
  );
}
const EdgeSelector = ({ connection, connections, onChange }) => {
  const options = connections.map(({name, unit}, i) => ({label: `${name} (${unit})`, value: i, name, unit}));
  return (
    <Select options={options}
            isMulti={false}
            defaultValue={options.find(({name, unit}) => name === connection.name && unit === connection.unit)}
            hideSelectedOptions={true}
            onChange={e => {
              debugger;
              onChange(e);
            }}
            placeholder={`Select ${name}`}
            closeMenuOnSelect={true}
            components={{ Option: Single }}
    />
  )
};

const EdgeConnection = ({ connection, name, connections, onChange }) => (
  <Line name={name}>
    {connections.length > 1 ?
      <EdgeSelector connection={connection} connections={connections} onChange={onChange}/> :
      connections[0].name === connection.name ?
        <Single data={{name: connection.name, unit: connection.unit}}/> :
        <EdgeSelector connection={connection} connections={connections} onChange={onChange}/>
    }
  </Line>
);
export const EdgeDetails = withPropsAPI(({ propsAPI }) => {
  const [{ model }, ...x] = propsAPI.getSelected();
  const { errors, source, target, sourceAnchor, targetAnchor, id, ...rest } = model;
  const src = propsAPI.find(source).model;
  const dst = propsAPI.find(target).model;
  const srcConnections = src.node.connections;
  const sourceConnection = srcConnections[sourceAnchor];
  const dstConnections = dst.node.connections;
  const targetConnection = dstConnections[targetAnchor];
  const inputs = dst.node.inputs || [];
  const outputs = src.node.outputs || [];
  const edge = propsAPI.find(id);
  
  const update = (field, f) => val => {
    debugger;
    propsAPI.update(edge, {
      ...model,
      [field]: !f ? val : f(val)
    });
  };
  
  return (
    <Card title={'Edge'}>
      <Description>
        <EdgeConnection name={'source'}
                        onChange={update('sourceAnchor', s => s + inputs.length)}
                        connections={outputs}
                        connection={sourceConnection}/>
        <EdgeConnection name={'target'}
                        onChange={update('targetAnchor')}
                        connections={inputs}
                        connection={targetConnection}/>
      </Description>
      <EdgeErrors errors={errors}
                  src={sourceConnection}
                  dst={targetConnection}/>
    </Card>
  );
});

