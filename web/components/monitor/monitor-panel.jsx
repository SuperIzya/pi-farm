import React from 'react';
import styles from './monitor-panel.scss';
import PropTypes from 'prop-types';
import { connectMonitorsContainer } from './index';
import ViewComponent from './view';

const PanelControl = ({ monitors, valueComponent }) => (
  <div className={styles.container}>
    {
      monitors.map(m => <ViewComponent key={m}
                                       monitor={m}
                                       valueComponent={valueComponent}/>)
    }
  </div>
);

PanelControl.propTypes = {
  monitors: PropTypes.arrayOf(PropTypes.string),
  monitorControl: PropTypes.func
};

const MonitorPanel = connectMonitorsContainer(PanelControl);
export default MonitorPanel;
