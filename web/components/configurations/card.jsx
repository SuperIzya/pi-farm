import React from 'react';
import styles from './card.scss';

export const Card = ({title, children}) => (
  <div className={styles.container}>
    <div className={styles.title}>{title}</div>
    <div className={styles.content}>{children}</div>
  </div>
);

export const Description = ({children}) => <div className={styles.description}>{children}</div>;

export const Line = ({name, value, children}) => (
  <React.Fragment>
    <div className={styles.label}>{name}</div>
    <div className={styles.value}>{children || value}</div>
  </React.Fragment>
);
