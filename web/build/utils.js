module.exports = {
  hash: (env, name, ext) => env === 'dev' ? `${name}.${ext}` : `${name}-[hash].${ext}`,
};
