import React from 'react';
import { connectMonitorValue, connectMonitorValueSeconds } from './index';

const Value = ({ monitor, index, value }) => (
  <div className={styles.container}>
  </div>
);

export const MinuteValue = connectMonitorValue(Value);
export const SecondValue = connectMonitorValueSeconds(Value);