import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import style from './dial.scss';
import DialContext from './context.js';

const filterPrint = numbersToPrint => {
  if (_.isFunction(numbersToPrint))
    return numbersToPrint;
  else if (_.isArray(numbersToPrint))
    return n => numbersToPrint.indexOf(n) > -1;
  else if (_.isObject(numbersToPrint))
    return n => !!numbersToPrint[n];
  return () => true;
};

const Section = ({ angle, radius, center, number, filter }) => (
  <g transform={`rotate(${angle} ${center.x} ${center.y})`}>
    <line x1={center.x + radius} y1={center.y}
          x2={center.x + radius + 5} y2={center.y}
          stroke="#666"
    />
    {!filter(number) ? null : (
      <text x={center.x + radius + 10} y={center.y} className={style.smallText}
            transform={`rotate(${-angle} ${center.x + radius + 16} ${center.y})`}
      >
        {number}
      </text>
    )}
  </g>
);


const Dial = ({ from, to, step, numbersToPrint, children }) => {
  const range = to - from;
  const steps = range / step;
  const radius = 60;
  const leftPoint = { x: 10, y: 50 };
  const rightPoint = { x: 110, y: 50 };
  const length = (rightPoint.x - leftPoint.x) / 2;
  const center = { x: leftPoint.x + length, y: leftPoint.y + Math.sqrt(radius * radius - length * length) };
  
  const totalAngle = Math.asin(length / radius) * (360 / Math.PI);
  const angleStep = totalAngle / steps;
  const leftAngle = -(totalAngle + (180 - totalAngle) / 2);
  
  const filter = filterPrint(numbersToPrint);
  
  const context = {
    steps,
    from,
    to,
    radius,
    leftPoint,
    rightPoint,
    center,
    totalAngle,
    angleStep,
    leftAngle,
    filter
  };
  
  return (
    <DialContext.Provider value={context}>
      <svg width="120" height="120" className={style.dial}>
        <path d={`
        M ${leftPoint.x} ${leftPoint.y}
   			A ${radius} ${radius} 0 0 1 ${rightPoint.x} ${rightPoint.y}
   			`}
              stroke="black"
              strokeWidth="1"
              fill="transparent"
        />
        <circle cx={center.x} cy={center.y} r="1" stroke="red" strokeWidth="2" fill="transparent"/>
        {
          [...Array(steps + 1).keys()]
          .map(i => <Section key={i}
                             radius={radius}
                             center={center}
                             number={from + i * step}
                             angle={leftAngle + angleStep * i}
                             filter={filter}
            />
          )
        }
        {children}
        Sorry, your browser does not support inline SVG.
      </svg>
    </DialContext.Provider>
  );
};

Dial.propTypes = {
  from: PropTypes.number.isRequired,
  to: PropTypes.number.isRequired,
  step: PropTypes.number.isRequired,
  numbersToPrint: PropTypes.any,
  children: PropTypes.oneOfType([
    PropTypes.element,
    PropTypes.arrayOf(PropTypes.element)
  ])
};

export default Dial;

