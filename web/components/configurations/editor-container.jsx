import React from 'react';
import GGEditor, { withPropsAPI } from 'gg-editor';

GGEditor.setTrackable(false);

class EditorContComponent extends React.PureComponent {
  
  log = name => ({ command }) => {
    console.log(name, command)
  };
  
  before = ({command}) => {
    const {type, name} = command;
    console.log(command);
    if (type === 'edge') {
      if(name === 'add') {
      
      }
    } else {
    }
  };
  
  render() {
    const { children, className } = this.props;
    return (
      <GGEditor className={className}
                onBeforeCommandExecute={this.before}>
        {children}
      </GGEditor>
    );
  }
}

export const EditorContainer = withPropsAPI(EditorContComponent);
