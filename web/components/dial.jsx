import React from 'react';

class Dial extends React.Component {
  
  render() {
    return (
      <canvas ref={this.canvas}>
        Dial
      </canvas>
    );
  }
  
  componentDidMount() {
  }
}

export default Dial;

