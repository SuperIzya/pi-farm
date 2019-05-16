const wp = require('../../webpack.config');
const path = require('path');
const outputDir = path.resolve(__dirname, 'src', 'main', 'resources', 'interface');
const CleanWebpackPlugin = require('clean-webpack-plugin');
console.log(outputDir);
module.exports = env => {
  const res = wp(env);
  return {
    ...res,
    entry: path.resolve(__dirname, 'web', 'motor.jsx'),
    output: {
      ...res.output,
      path: outputDir,
      filename: 'motor.js'
    },
    devServer: {
      contentBase: outputDir,
    },
    plugins: [
      new CleanWebpackPlugin(outputDir),
      ...res.plugins.slice(2)
    ]
  };
};