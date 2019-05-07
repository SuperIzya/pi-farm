import React from 'react';
import style from './mini-board.scss';

export const MiniBoard = ({ device, driver }) => (
  <span className={style.text}>
    {device} Mini board {driver}
  </span>
);
