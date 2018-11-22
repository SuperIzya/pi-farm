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
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: [
              '@babel/preset-env',
              '@babel/preset-react'
            ],
            plugins: [
              '@babel/plugin-proposal-object-rest-spread',
              'transform-class-properties',
              [require('babel-plugin-react-css-modules'), {
                generateScopedName: '[path]_[name]_[hash:base64]',
                webpackHotModuleReloading: true,
                filetypes: {
                  ".scss": {
                    syntax: "postcss-scss",
                    plugins: ["postcss-nested"]
                  }
                }
              }]
            ]
          }
        }
      }, {
        test: /\.scss$/,
        exclude: /node_modules/,
        use: [{
          loader: "style-loader",
          options: {
            sourceMap: true,
          }
        }, {
          loader: 'css-loader',
          options: {
            sourceMap: true,
            localIdentName: '[name]__[local]___[hash:base64:5]',
            importLoaders: 2,
            module: true
          }
        }, {
          loader: 'postcss-loader',
          options: {
            sourceMap: true
          }
        }, {
          loader: 'sass-loader',
          options: {
            sourceMap: true
          }
        }]
      }, {
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
      }
    ]
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
