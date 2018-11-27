import React from 'react';
import Log from './components/log';
import style from './app.scss';
import store from './store/store';
import { Provider } from 'react-redux';
import Boards from './components/boards';
import { hot, setConfig } from 'react-hot-loader';

setConfig({
  ignoreSFC: true, // RHL will be __complitely__ disabled for SFC
  pureRender: true, // RHL will not change render method
})
const AppComponent = () => (
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

const App = window.environment === 'dev' ? hot(module)(AppComponent) : AppComponent;

export default App;


