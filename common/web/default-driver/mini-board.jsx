import React from 'react';

export const MiniBoard = ({ device, driver }) => (
  <span className={style.text}>
    {device} Mini board {driver}
  </span>
);
