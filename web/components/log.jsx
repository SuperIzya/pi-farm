import React from 'react';
import style from './log.scss';
import {connectToLog} from './log.js';


const Log = ({logs}) => (
  <div className={style.container}>
    <div className={style.header}>Arduino log</div>
    <div className={style.control}>
    
    </div>
    <div className={style.logs}>
      {logs.map((l, i) => <span key={i}>{l}</span>)}
    </div>
  </div>
);


export default connectToLog(Log);


