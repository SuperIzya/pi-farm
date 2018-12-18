import React from 'react';
import Log from './components/log';
import style from './app.scss';
import store from './store/store';
import { Provider } from 'react-redux';
import Boards from './components/boards';
import MonitorPanel from './components/monitor/monitor-panel';
import { MinuteValue, SecondValue } from './components/monitor/value';

const App = () => (
  <div className={style.container}>
    <Provider store={store}>
      <div className={style.content}>
        <div className={style.boards}><Boards/></div>
        <div className={style.logs}>
          <MonitorPanel valueComponent={MinuteValue}/>
          <MonitorPanel valueComponent={SecondValue}/>
        </div>
      </div>
    </Provider>
  </div>
);

window.onclose = () => window.closing = true;

export default App;


