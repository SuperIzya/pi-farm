import React from 'react';
import { HistorySpan } from './store';
import styles from './view.scss';

const src = Array(HistorySpan).map(Number.call, Number);

const ViewComponent = ({ monitor, valueComponent }) => (
  <div className={styles.container}>
    {src.map(index => valueComponent({ monitor, index }))}
  </div>
);

export default ViewComponent;
