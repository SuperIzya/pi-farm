import React from 'react';
import Log from './components/log';
import style from './app.scss';
import store from './store/store';
import {Provider} from 'react-redux';

const App = () => (
  <div className={style.container}>
    <Provider store={store}>
      <Log/>
    </Provider>
  </div>
);


export default App;

