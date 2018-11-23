import {combineReducers, createStore, applyMiddleware} from 'redux';
import {reducerRegistry} from './utils';
import { createEpicMiddleware } from 'redux-observable';
import {rootEpic} from './epics';
import { composeWithDevTools } from 'redux-devtools-extension/developmentOnly';

const reducers = combineReducers(reducerRegistry.getReducers());

const epicMiddleware = createEpicMiddleware();

const composeEnhancers = composeWithDevTools;

const store = createStore(reducers, {}, composeEnhancers(applyMiddleware(epicMiddleware)));
reducerRegistry.setChangeListener(reducers => store.replaceReducer(combineReducers(reducers)));
store.replaceReducer(combineReducers(reducerRegistry.getReducers()));
epicMiddleware.run(rootEpic);
export default store;
