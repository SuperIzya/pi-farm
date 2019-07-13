import React from 'react';
import classNames from 'classnames';
import styles from './ellipsised-text.scss';
export const EllipsisedText = ({className, text}) => (
  <div className={classNames(className, styles.container)}
       title={text}>
    {text}
  </div>
);
