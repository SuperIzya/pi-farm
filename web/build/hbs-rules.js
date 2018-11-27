module.exports = env => [{
  test: /\.hbs$/,
  use: [{
    loader: "html-loader",
    options: {
      attrs: ["link:href"],
      interpolate: true
    }
  }, {
    loader: 'handlebars-render-loader',
    options: {
      data: {
        env,
        port: 8080
      }
    }
  }]
}];
