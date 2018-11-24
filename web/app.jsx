import React from 'react';
import Log from './components/log';
import style from './app.scss';
import store from './store/store';
import {Provider} from 'react-redux';
import Boards from './components/boards';

const App = () => (
  <div className={style.container}>
    <Provider store={store}>
      <div className={style.content}>
        <div><Boards/></div>
        <div><Log/></div>
      </div>
    </Provider>
  </div>
);


export default App;

