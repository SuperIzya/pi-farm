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

const LogComponent = ({ filters, maxLogs, setMaxLogs }) => (
  <div className={style.log}>
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

class Log extends React.Component {
  state = {
    closed: true
  };
  
  onClick = () => this.setState({closed: !this.state.closed});
  render() {
    return (
      <div className={style.container}>
        <div className={style.button} onClick={this.onClick}>
          {this.state.closed ? 'Show logs' : 'Hide logs'}
        </div>
        {this.state.closed ? null : <LogComponent {...this.props}/>}
      </div>
    );
  }
}

export default connectToLog(Log);


