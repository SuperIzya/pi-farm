'use strict';

const Webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');
const webpackConfig = require('../webpack.config')('dev');

const compiler = Webpack(webpackConfig);
const devServerOptions = Object.assign({}, webpackConfig.devServer, {
  stats: {
    colors: true
  },
  open: 'google-chrome',
  port: 9000,
});
const server = new WebpackDevServer(compiler, devServerOptions);
const port = parseInt(process.argv[2]);

server.listen(port, '0.0.0.0', () => {
  console.log(`Starting server on http://localhost:${port}`);
});