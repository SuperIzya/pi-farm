import React from 'react';
import styles from './nodes.scss';
import { connectNode, connectNodesList } from './store';
import { Subject, from } from 'rxjs';
import socket from '../../utils/socket';
import { takeUntil, map } from 'rxjs/operators';
import { ofType } from 'redux-observable';
import { Item, withPropsAPI } from 'gg-editor';

const fontSize = '12px';

const textWrap = (t, width) => {
  const content = t.innerHTML;
  const abc = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  t.setAttribute('text-anchor', 'start');
  t.innerHTML = abc;
  const letterWidth = t.getBBox().width / abc.length;
  t.innerHTML = content;
  
  const breakWord = ' -_.,;:(){}=+/&*'.split('');
  const maxLetters = Math.floor(width / letterWidth);
  
  const split = word => {
    const br = breakWord
    .map(c => ({
      char: c,
      index: word.lastIndexOf(c)
    }))
    .filter(({ index }) => index > -1)
    .sort(({ index: i1 }, { index: i2 }) => i1 < i2 ? 1 : (i1 === i2 ? 0 : -1));
    
    if (br.length) {
      const { index, char } = br[0];
      return [
        [...word.slice(0, index + (char === ' ' ? 0 : 1)), '\n'],
        [...word.slice(index + 1)],
      ]
    }
    
    return [
      [''],
      word
    ]
  };
  
  const html = content.split('').reduce((a, w) => {
    const top = a[a.length - 1];
    const rest = a.slice(0, a.length - 1);
    const l = top.length + 1;
    if (l > maxLetters) return [...rest, ...split([...top, w])];
    return [...rest, [...top, w]];
  }, [[]])
  .map(a => a.join(''))
  .map((s, i) => `<tspan x="8px" dy="${(i && fontSize) || 0}">${s}</tspan>`)
  .join('');
  
  const {width: w, height: h} = t.getBBox();
  return {
    x: `${(width - w) / 2}px`,
    y: `${(50 - h) / 2}px`,
    content: html
  };
  
};

class NodeImgComponent extends React.Component {
  state = {
    x: '0px',
    y: '0px'
  };
  
  ref = React.createRef();
  
  componentDidMount() {
    const {x, y, content} = textWrap(this.ref.current, 80);
    
    this.setState({x, y}, () => this.ref.current.innerHTML = content);
  }
  
  render() {
    const { name } = this.props;
    return (
      <svg width="88" height="56">
        <g transform="translate(4 2)" fill={'none'}>
          <filter x="-8.8%"
                  y="-10.4%"
                  width="117.5%"
                  height="129.2%"
                  filterUnits="objectBoundingBox"
                  fill="none" fillRule="evenodd">
            <feOffset dy="2" in="SourceAlpha" result="shadowOffsetOuter1"/>
            <feGaussianBlur stdDeviation="2" in="shadowOffsetOuter1" result="shadowBlurOuter1"/>
            <feComposite in="shadowBlurOuter1" in2="SourceAlpha" operator="out" result="shadowBlurOuter1"/>
            <feColorMatrix values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.04 0" in="shadowBlurOuter1"/>
          </filter>
          <rect fillOpacity=".92" fill="#E6F7FF" x="0" y="0" width="80" height="48" rx="4"/>
          <rect stroke="#1890FF" x=".5" y=".5" width="79" height="47" rx="4"/>
        </g>
        <text
          ref={this.ref}
          x={this.state.x}
          y={this.state.y}
          fontSize={fontSize}
          fill="#000"
          fillOpacity=".65"
        >
          {name}
        </text>
      </svg>
    );
  }
}

const NodeImg = withPropsAPI(NodeImgComponent);

const NodeComponent = ({ node }) => {
  const {name} = node;
  return (
    <Item type={'node'}
          size={'80*40'}
          shape={name}
          model={{
            name,
            label: name,
            node: {...node}
          }}
    >
      <NodeImg name={name}/>
    </Item>
  );
}

const Node = connectNode(NodeComponent);

class NodesComponent extends React.PureComponent {
  unmount = new Subject();
  
  componentWillUnmount() {
    this.unmount.next();
  }
  
  componentWillMount() {
    socket.send({ type: 'configuration-nodes-get' });
    socket.messages.pipe(
      takeUntil(this.unmount),
      ofType('configuration-nodes'),
      map(x => x.nodes)
    ).subscribe(d => this.props.onData(d))
  }
  
  render() {
    const { count } = this.props;
    return (
      <div className={styles.container}>
        {new Array(count).fill(0).map((x, i) => <Node index={i} key={i}/>)}
      </div>
    );
  }
}

export const Nodes = connectNodesList(NodesComponent);


