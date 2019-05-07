module.exports = [{
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
        '@babel/plugin-syntax-dynamic-import',
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
},{
  test: /^file:\/\//,
  use: {
    loader: 'bundle-loader',
    options: {
      lazy: false
    }
  }
}];
