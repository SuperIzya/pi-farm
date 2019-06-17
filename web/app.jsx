import React from 'react';
import Log from './components/log';
import style from './app.scss';
import store from './store/store';
import { Provider } from 'react-redux';
import Boards from './components/boards/boards';
import { Route, Switch } from 'react-router';
import { BrowserRouter, NavLink } from 'react-router-dom';
import { GlobalLoader } from './utils/loader';
import { BoardsIcon, ConfigurationIcon } from './icons';

const App = () => (
  <div className={style.container}>
    <Provider store={store}>
      <GlobalLoader>
        <div className={style.content}>
          <BrowserRouter>
            <div className={style.navigation}>
              <NavLink className={style.link}
                       activeClassName={style.active}
                       to={'/'}
                       exact={true}>
                <BoardsIcon/>Boards
              </NavLink>
              <NavLink className={style.link}
                       activeClassName={style.active}
                       to={'/configurations'}>
                <ConfigurationIcon/>Configurations
              </NavLink>
                       
            </div>
            
            <Switch>
              <Route exact={true} path={'/'}>
                <Boards/>
              </Route>
              <Route path={'/board?board'}>
                <div className={style.board}>Single board</div>
              </Route>
            </Switch>
          </BrowserRouter>
        </div>
        <Log/>
      </GlobalLoader>
    </Provider>
  </div>
);

window.onclose = () => window.closing = true;

export default App;


