import React from 'react';
import Log from './components/log';
import style from './app.scss';
import store from './store/store';
import { Provider } from 'react-redux';
import Boards from './components/boards';

const App = () => (
  <div className={style.container}>
    <Provider store={store}>
      <div className={style.content}>
        <div className={style.boards}><Boards/></div>
        <div className={style.logs}><Log/></div>
      </div>
    </Provider>
  </div>
);

window.onclose = () => window.closing = true;

export default App;


