
const CSSModuleLoader = {
  loader: 'css-loader',
  options: {
    modules: true,
    sourceMap: true,
    localIdentName: '[name]__[local]___[hash:base64:5]',
    minimize: true
  }
};

const postCSSLoader = {
  loader: 'postcss-loader',
  options: {
    ident: 'postcss',
    sourceMap: true
  }
};

const styleLoader = {
  loader: "style-loader",
  options: {
    sourceMap: true,
  }
};

const sassLoader = {
  loader: 'sass-loader',
  options: {
    sourceMap: true
  }
};


module.exports = [{
  test: /\.scss$/,
  exclude: /(node_modules|style)/,
  use: [
    styleLoader,
    CSSModuleLoader,
    postCSSLoader,
    sassLoader
  ]
}];

