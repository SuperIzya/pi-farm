import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';

const filterPrint = numbersToPrint => {
  if(_.isObject(numbersToPrint))
    return n => !!numbersToPrint[n];
  else if(_.isArray(numbersToPrint))
    return n => numbersToPrint.indexOf(n) > -1;
  else if(_.isFunction(numbersToPrint))
    return numbersToPrint;
};

const Dial = ({ from, to, step, numbersToPrint }) => (
  <svg width="100" height="100">
    <path d="M 10 50
   			A 60 60 0 0 1 90 50"
          stroke="black"
          strokeWidth="1"
          fill="transparent"
  
    />
    <circle cx="50" cy="94" r="1" stroke="red" strokeWidth="2" fill="transparent"/>
    Sorry, your browser does not support inline SVG.
  </svg>
);

Dial.propTypes = {
  from: PropTypes.number.isRequired,
  to: PropTypes.number.isRequired,
  step: PropTypes.number.isRequired,
  numbersToPrint: PropTypes.any
};

export default Dial;

