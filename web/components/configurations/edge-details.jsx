import React from 'react';
import styles from './edge-details.scss';
import { withPropsAPI } from "gg-editor";
import { Card, Description, Line } from './card';
import { EllipsisedText } from './ellipsised-text';
import Select from 'react-select';
import _ from 'lodash';


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
};

const EdgeSelector = ({ connection, connections, onChange, name }) => {
  const options = connections.map(({name, unit}, i) => ({label: `${name} (${unit})`, value: i, name, unit}));
  const val = options.find(({name, unit}) => name === connection.name && unit === connection.unit);
  return (
    <Select options={options}
            isMulti={false}
            defaultValue={val}
            hideSelectedOptions={true}
            onChange={onChange}
            placeholder={`Select ${name}`}
            closeMenuOnSelect={true}
            components={{ Option: Single }}
    />
  )
};

const EdgeConnection = ({ connection, name, connections, onChange }) => (
  <Line name={name}>
    {connections.length > 1 ?
      <EdgeSelector connection={connection} name={name} connections={connections} onChange={onChange}/> :
      connections[0].name === connection.name ?
        <Single data={{name: connection.name, unit: connection.unit}}/> :
        <EdgeSelector connection={connection} name={name} connections={connections} onChange={onChange}/>
    }
  </Line>
);
class EdgeDetailsComponent extends React.Component {
  state = {};
  
  calcState = ({propsAPI}) => {
    if(!propsAPI) return;
    const [{ model }, ...x] = propsAPI.getSelected();
    const { errors, source, target, sourceAnchor, targetAnchor, id } = model;
    const src = propsAPI.find(source).model;
    const dst = propsAPI.find(target).model;
    const srcConnections = src.node.connections;
    const sourceConnection = srcConnections[sourceAnchor];
    const dstConnections = dst.node.connections;
    const targetConnection = dstConnections[targetAnchor];
    const inputs = dst.node.inputs || [];
    const outputs = src.node.outputs || [];
    const edge = propsAPI.find(id);
  
    this.update = (field, f) => val => {
      propsAPI.update(edge, {
        ...model,
        [field]: !f ? val : f(val)
      });
      this.calcState(this.props);
    };
    this.setState({
      sourceConnection,
      targetConnection,
      errors,
      inputs,
      outputs
    });
  };
  
  componentDidMount() {
    this.calcState(this.props);
  }
  
  componentWillReceiveProps(nextProps, nextContext) {
    this.calcState(nextProps);
  }
  
  render() {
    if(_.isEmpty(this.state)) return null;
    const {errors, inputs, outputs, sourceConnection, targetConnection} = this.state;
    
    return (
      <Card title={'Edge'}>
        <Description>
          <EdgeConnection name={'source'}
                          onChange={this.update('sourceAnchor', s => s + inputs.length)}
                          connections={outputs}
                          connection={sourceConnection}/>
          <EdgeConnection name={'target'}
                          onChange={this.update('targetAnchor')}
                          connections={inputs}
                          connection={targetConnection}/>
        </Description>
        <EdgeErrors errors={errors}
                    src={sourceConnection}
                    dst={targetConnection}/>
      </Card>
    );
  }
}

export const EdgeDetails = withPropsAPI(EdgeDetailsComponent);
