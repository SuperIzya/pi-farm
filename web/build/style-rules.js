const path = require('path');

module.exports = [{
  test: /\.scss$/,
  include: /style/,
  use: [
    {
      loader: "file-loader",
      options: {
        name: path.join('web', '[name]-[hash].css'),
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
