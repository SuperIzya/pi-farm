import React from 'react';
import Log from './components/log';
import style from './app.scss';
import store from './store/store';
import { Provider } from 'react-redux';
import Boards from './components/boards/boards';
import { Route, Switch } from 'react-router';
import { BrowserRouter } from 'react-router-dom';
import { GlobalLoader } from './utils/loader';

const App = () => (
  <div className={style.container}>
    <Provider store={store}>
      <GlobalLoader>
        <div className={style.content}>
          <BrowserRouter>
            <Switch>
              <Route exact={true} path={'/'}>
                <div className={style.boards}><Boards/></div>
              </Route>
              <Route path={'/board?board'}>
                <div className={style.board}>Single board</div>
              </Route>
            </Switch>
          </BrowserRouter>
        </div>
        <div className={style.logs}><Log/></div>
      </GlobalLoader>
    </Provider>
  </div>
);

window.onclose = () => window.closing = true;

export default App;


