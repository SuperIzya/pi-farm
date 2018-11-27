const path = require('path');
const CircularDependencyPlugin = require('circular-dependency-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const webpack = require('webpack');
const utils = require('./web/build/utils');

const outputDir = path.resolve(__dirname, 'src', 'main', 'resources', 'interface');
const rules = env => [].concat(
  require('./web/build/js-rules'),
  require('./web/build/style-rules')(env),
  require('./web/build/module-style-rules'),
  require('./web/build/hbs-rules')(env),
);

const plugins = env => [
  new HtmlWebpackPlugin({
    inject: 'body',
    template: 'index.hbs',
    filename: 'index.html',
  }),
  new CircularDependencyPlugin({
    // exclude detection of files based on a RegExp
    exclude: /\.js$|\/node_modules\//,
    // add errors to webpack instead of warnings
    failOnError: true,
    // set the current working directory for displaying module paths
    cwd: process.cwd(),
  }),
  new CleanWebpackPlugin(outputDir),
].concat(
  env !== 'dev' ? [] : [
    new webpack.HotModuleReplacementPlugin()
  ]
);

module.exports = env => ({
  entry: path.resolve(__dirname, 'web', 'index.jsx'),
  output: {
    path: outputDir,
    publicPath: '/',
    filename: path.join('web', utils.hash(env, 'bundle', 'js'))
  },
  devServer: {
    contentBase: outputDir,
  },
  context: path.resolve(__dirname, 'web'),
  mode: 'development',
  devtool: 'source-map',
  module: { rules: rules(env) },
  resolve: {
    extensions: ['*', '.jsx', '.js']
  },
  plugins: plugins(env)
});
