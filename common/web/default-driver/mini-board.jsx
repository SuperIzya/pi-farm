import React from 'react';
import style from './mini-board.scss';
import classNames from 'classnames';
import { Button } from '@material-ui/core';

export const MiniBoard = ({ device, driver, send }) => (
  <div className={classNames(style.container, {[style.selected]: false})}>
    <div className={style.buttonContainer}>
      <Button className={style.button}
              variant="contained"
              color="primary"
              onClick={() => send({type: 'the-led', value: true})}>
        Led on/off
      </Button>
    </div>
    <span className={style.text}>
      {device} Mini board {driver}
    </span>
  </div>
);
