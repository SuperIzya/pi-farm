'use strict';

const Webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');
const webpackConfig = require('../webpack.config')('dev');

const compiler = Webpack(webpackConfig);
const port = parseInt(process.argv[2]);
const devServerOptions = Object.assign({}, webpackConfig.devServer, {
  stats: {
    colors: true
  },
  open: 'google-chrome',
  port,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      logLevel: 'debug',
      changeOrigin: true,
      toProxy: true,
      ignorePath: false,
      prependPath: true,
      secure: false
    },
    
  }
});
const server = new WebpackDevServer(compiler, devServerOptions);

server.listen(port, '0.0.0.0', () => {
  console.log(`Starting server on http://localhost:${port}`);
});