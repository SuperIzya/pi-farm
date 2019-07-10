import React from 'react';
import { Command, Toolbar } from 'gg-editor';
import styles from './editor-toolbar.scss';

const IconFont = ({ type, text }) => (
  <div className={styles.icon}>
    <i className={'material-icons'} aria-label={text || type}>{type}</i>
  </div>
);

const ToolbarButton = (props) => {
  const { command, icon, text } = props;
  
  return (
    <Command name={command}>
      <IconFont type={icon || command} text={text}/>
    </Command>
  );
};


export const EditorToolbar = () => {
  return (
    <Toolbar className={styles.container}>
      <ToolbarButton command="undo"/>
      <ToolbarButton command="redo"/>
      <div className={styles.divider}/>
      <ToolbarButton command="copy" icon={'filter_none'}/>
      <ToolbarButton command="paste" icon={'assignment_turned_in'}/>
      <ToolbarButton command="delete"/>
      <div className={styles.divider}/>
      <ToolbarButton command="zoomIn" icon="zoom_in" text="Zoom In"/>
      <ToolbarButton command="zoomOut" icon="zoom_out" text="Zoom Out"/>
      <ToolbarButton command="autoZoom" icon="fullscreen_exit" text="Fit Map"/>
      <ToolbarButton command="resetZoom" icon="fullscreen" text="Actual Size"/>
      <div className={styles.divider}/>
      <ToolbarButton command="toBack" icon="flip_to_back" text="To Back"/>
      <ToolbarButton command="toFront" icon="flip_to_front" text="To Front"/>
      {/*<div className={styles.divider}/>
      <ToolbarButton command="multiSelect" icon="multi-select" text="Multi Select"/>
      <ToolbarButton command="addGroup" icon="group" text="Add Group"/>
      <ToolbarButton command="unGroup" icon="ungroup" text="Ungroup"/>*/}
    </Toolbar>
  );
};
