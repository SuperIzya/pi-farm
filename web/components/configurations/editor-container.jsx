import React from 'react';
import GGEditor, { withPropsAPI } from 'gg-editor';
import { Subject } from 'rxjs';
import { takeUntil, take, filter, map, tap } from 'rxjs/operators';
import _ from 'lodash';

GGEditor.setTrackable(false);

const testObject = (obj, cmd, field) => {
  const f = !cmd[field] ? null : obj[cmd[field]];
  if(f) return f(cmd);
  return {};
};

const addObj = propsAPI => ({
  'edge': ({ addModel: {source, target, sourceAnchor, targetAnchor} }) => {
    const { model: { node: src } } = propsAPI.find(source);
    const { model: { node: dst } } = propsAPI.find(target);
    let errors = [];
    if(!_.isNumber(sourceAnchor) || !_.isNumber(targetAnchor) ||
      src.anchors[sourceAnchor][1] <= dst.anchors[targetAnchor][1]) {
      errors = [...errors, 'Should connect output with input!'];
    }
    
    if(_.isNumber(sourceAnchor) && _.isNumber(targetAnchor) &&
      src.connections[sourceAnchor].unit !== dst.connections[targetAnchor].unit) {
      errors = [...errors, 'Should connect same units'];
    }
    
    return {errors};
  }
});

const testObj = propsAPI => {
  const addO = addObj(propsAPI);
  return {
    'add': cmd => testObject(addO, cmd, 'type')
  };
};

const testCmd = propsAPI => {
  const obj = testObj(propsAPI);
  return command => testObject(obj, command, 'name');
};

class TesterComponent extends React.PureComponent {
  unmount = new Subject();
  
  procProps = ({ propsAPI, command$ }) => {
    this.testCmd = testCmd(propsAPI);
    this.unmount.next();
    if(command$) {
      const result$ = command$.pipe(
        takeUntil(this.unmount),
        map(x => ({
          ...x,
          ...this.testCmd(x)
        }))
      );
  
      result$.pipe(
        filter(({ errors }) => !!(errors || []).length)
      ).subscribe(x => {
        const id = x.addId || x.itemId;
        const item = propsAPI.find(id);
        if(item)
          propsAPI.update(item, {...item.model, errors: x.errors, style: {'stroke': 'red'}});
      });
      
      result$.pipe(
        filter(({errors}) => !(errors || []).length)
      ).subscribe(x => {
        const id = x.addId || x.itemId;
        const item = propsAPI.find(id);
        if(item) {
          const { errors, style, ...model } = item.model;
          propsAPI.update(item, model);
        }
      })
    }
  };
  
  componentWillReceiveProps(nextProps, nextContext) {
    this.procProps(nextProps);
  }
  
  componentWillMount() {
    this.procProps(this.props);
  }
  
  componentWillUnmount() {
    this.unmount.next();
  }
  
  render() {
    return this.props.children;
  }
  
}

const Tester = withPropsAPI(TesterComponent);

export class EditorContainer extends React.PureComponent {
  command$ = new Subject();
  
  before = ({ command }) => {
    this.command$.next(command);
    return command;
  };
  
  
  render() {
    const { children, className } = this.props;
    return (
      <GGEditor className={className}
                onAfterCommandExecute={this.before}>
        <Tester children={children} command$={this.command$}/>
      </GGEditor>
    );
  }
}

