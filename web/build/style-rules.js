const path = require('path');

const utils = require('./utils');

module.exports = env => [{
  test: /\.scss$/,
  include: /style/,
  use: [
    {
      loader: "file-loader",
      options: {
        name: path.join('web', utils.hash(env, '[name]', 'css')),
        useRelativePath: false,
        sourceMap: true
      }
    }, {
      loader: "extract-loader",
      options: {
        sourceMap: true
      }
    }, {
      loader: "css-loader",
      options: {
        sourceMap: true
      }
    }, {
      loader: 'resolve-url-loader',
      options: {
        sourceMap: true
      }
    }, {
      loader: 'sass-loader',
      options: {
        sourceMap: true
      }
    }
  ]
}];
