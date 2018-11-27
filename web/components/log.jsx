import React from 'react';
import style from './log.scss';
import { connectToLogList, connectToLog } from './log.js';
import moment from 'moment';

const toTime = ts => moment(ts).format('D/M/YYYY H:mm:ss.SSS');

const LogRow = ({ log }) => (
  <React.Fragment>
    <div className={style.timestamp}>[{toTime(log.timestamp)}]</div>
    <div className={style.log}>{log.log}</div>
  </React.Fragment>
);
const LogCollection = connectToLogList(({ logs }) => (
  <div className={style.logs}>
    {logs.map((l, i) => <LogRow key={i} log={l}/>)}
  </div>
));

const newMax = setMaxLogs => evt => setMaxLogs(parseInt(evt.target.value));

const Log = ({ filters, maxLogs, setMaxLogs }) => (
  <div className={style.container}>
    <div className={style.header}>Arduino log for {filters.join(', ')}</div>
    <div className={style.control}>
      <label>Max logs to store</label>
      <input type="number"
             value={maxLogs}
             onChange={newMax(setMaxLogs)}/>
    </div>
    <div className={style.scrollerContainer}>
      <div className={style.scroller}>
        <LogCollection/>
      </div>
    </div>
    <div/>
  </div>
);


export default connectToLog(Log);


