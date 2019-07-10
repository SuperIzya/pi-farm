import React from 'react';
import styles from './card.scss';

export const Card = ({title, children}) => (
  <div className={styles.container}>
    <div className={styles.title}>{title}</div>
    <div className={styles.content}>{children}</div>
  </div>
);
