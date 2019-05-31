import React from 'react';
import PropTypes from 'prop-types';
import DialContext from './context.js';
import style from './hand.scss';

class Hand extends React.PureComponent {
  render() {
    const {center, rightPoint, from, to, leftAngle, totalAngle} = this.context;
    const { value, style: customStyle } = this.props;
    const val = Math.max(Math.min(value, to - 1), from + 1);
    const handAngle = leftAngle + (val - from) / (to - from) * totalAngle;
    
    return (
      <line id="hand"
            x1={center.x - 5} y1={center.y}
            x2={rightPoint.x + 5} y2={center.y}
            className={`${style.hand} ${customStyle}`}
            stroke="#888"
            transform={`rotate(${handAngle} ${center.x} ${center.y})`}/>
    );
  }
}
Hand.contextType = DialContext;
Hand.propTypes = {
  value: PropTypes.number.isRequired,
  style: PropTypes.string,
};

export default Hand;
