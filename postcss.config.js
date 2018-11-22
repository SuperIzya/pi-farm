module.exports = {
    generateScopedName: '[name]__[local]___[hash:base64:5]',
    plugins: [
        require('precss'),
        require('autoprefixer')
    ]
}
