'use strict';
const fs = require('fs');
const Webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');
const webpackConfig = require('../webpack.config')('dev');
const path = require('path');

const compiler = Webpack(webpackConfig);
const port = parseInt(process.argv[2]);
const resources = module => path.resolve(path.join(__dirname, '..', module, 'src', 'main', 'resources'));
const pluginContent = new RegExp('/api/get-plugin/file:/.+/([^/]+)_([\\d\\-\\.])*\\.jar!');
const devServerOptions = Object.assign({}, webpackConfig.devServer, {
  stats: {
    colors: true
  },
  open: 'google-chrome',
  port,
  proxy: {
    '/api/get-plugin': {
      target: 'http://localhost:8080/get-plugin',
      logLevel: 'debug',
      onProxyReq: (proxyReq, req, res) => {
        if(pluginContent.test(req.url)) {
          const m = req.url.match(pluginContent);
          const file = resources(m[1]) + req.url.split('!')[1];
          res.writeHead(200, { 'Content-Type': 'text/plain' });
          res.end(fs.readFileSync(file));
        }
      }
    },
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