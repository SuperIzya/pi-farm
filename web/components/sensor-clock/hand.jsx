import React from 'react';
import PropTypes from 'prop-types';
import DialContext from './context.js';

class Hand extends React.PureComponent {
  render() {
    const {center, rightPoint, from, to, leftAngle, totalAngle} = this.context;
    const value = Math.max(Math.min(this.props.value, to - 1), from + 1);
    const handAngle = leftAngle + (value - from) / (to - from) * totalAngle;
    
    return (
      <line id="hand"
            x1={center.x - 5} y1={center.y}
            x2={rightPoint.x + 5} y2={center.y}
            stroke="#888"
            transform={`rotate(${handAngle} ${center.x} ${center.y})`}/>
    );
  }
}
Hand.contextType = DialContext;
Hand.propTypes = {
  value: PropTypes.number.isRequired
};

export default Hand;
