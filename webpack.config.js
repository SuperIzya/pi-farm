const path = require('path');
const CircularDependencyPlugin = require('circular-dependency-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const outputDir = path.resolve(__dirname, 'src', 'main', 'resources', 'web');

module.exports = {
  entry: path.resolve(__dirname, 'web', 'index.jsx'),
  output: {
    path: outputDir,
    publicPath: '/web',
    filename: 'bundle.js'
  },
  context: path.resolve(__dirname, 'web'),
  mode: 'development',
  devtool: 'source-map',
  module: {
    rules: [].concat(
      require('./web/build/js-rules'),
      require('./web/build/style-rules'),
      require('./web/build/module-style-rules'),
      [{
        test: /\.hbs$/,
        use: [{
          loader: "html-loader",
          options: {
            attrs: ["link:href"],
            interpolate: true
          }
        }, {
          loader: 'handlebars-render-loader',
          
        }]
      }]
    )
  },
  resolve: {
    extensions: ['*', '.jsx', '.js']
  },
  plugins: [
    new HtmlWebpackPlugin({
      inject: 'body',
      template: 'index.hbs',
      filename: 'index.html'
    }),
    new CircularDependencyPlugin({
      // exclude detection of files based on a RegExp
      exclude: /\.js$|\/node_modules\//,
      // add errors to webpack instead of warnings
      failOnError: true,
      // set the current working directory for displaying module paths
      cwd: process.cwd(),
    })
  ]
};
