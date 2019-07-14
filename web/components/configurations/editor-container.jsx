import React from 'react';
import GGEditor, { withPropsAPI } from 'gg-editor';
import { Subject } from 'rxjs';
import { takeUntil, take, filter, map, tap } from 'rxjs/operators';
import _ from 'lodash';
import { EditorContext } from './editor-context';

GGEditor.setTrackable(false);

const testEdge = propsAPI => ({ model }) => {
  const { source, target, sourceAnchor, targetAnchor } = model;
  const { model: { node: src } } = propsAPI.find(source);
  const { model: { node: dst } } = propsAPI.find(target);
  let errors = [];
  if (!_.isNumber(sourceAnchor) || !_.isNumber(targetAnchor) ||
    src.anchors[sourceAnchor][1] <= dst.anchors[targetAnchor][1]) {
    errors = [...errors, 'Should connect output with input!'];
  }
  
  if (_.isNumber(sourceAnchor) && _.isNumber(targetAnchor) &&
    src.connections[sourceAnchor].unit !== dst.connections[targetAnchor].unit) {
    errors = [...errors, 'Should connect same units'];
  }
  
  return { ...model, errors };
};

const testObj = propsAPI => {
  const edge = testEdge(propsAPI);
  return {
    edge
  };
};

const testCmd = propsAPI => {
  const obj = testObj(propsAPI);
  
  return item => {
    const f = obj[item.type];
    if (f) return f(item);
    return item.model;
  }
};

class TesterComponent extends React.PureComponent {
  static contextType = EditorContext;
  
  unmount = new Subject();
  
  procProps = ({ propsAPI }) => {
    this.testCmd = testCmd(propsAPI);
    this.unmount.next();
    if (this.context) {
      this.context.pipe(
        takeUntil(this.unmount),
        map(x => propsAPI.find(x)),
        filter(x => !!x && !!x.type),
        map(x => ({ obj: x, model: this.testCmd(x) })),
        map(({ obj, model: { errors, ...rest } }) => ({
          obj,
          model: {
            ...rest,
            errors,
            style: (errors || []).length ? { stroke: 'red' } : {}
          }
        }))
      ).subscribe(({ obj, model }) => propsAPI.update(obj, model));
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
    const id = command.itemId || command.addId;
    this.command$.next(id);
    //  console.log(command);
    return command;
  };
  
  
  render() {
    const { children, className } = this.props;
    return (
      <GGEditor className={className}
                onAfterCommandExecute={this.before}>
        <EditorContext.Provider value={this.command$}>
          <Tester>
            {children}
          </Tester>
        </EditorContext.Provider>
      </GGEditor>
    );
  }
}

