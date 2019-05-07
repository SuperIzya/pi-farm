import React, { Suspense } from 'react';
import { connectBoard } from './store.js';
import Loading from '../../icons/loading';
import { Loaded, loadPlugin } from '../../utils/loader';

export const MiniBoardComponent = ({ device, driver, meta }) => {
  const Loader = loadPlugin(meta.index);
  const loading = <Loading/>;
  
  return !(meta && meta.mini) ? null : (
    <Loader fallback={loading}>
      <Loaded component={meta.mini} device={device} driver={driver}/>
    </Loader>
  );
};

export const MiniBoard = connectBoard(MiniBoardComponent);
