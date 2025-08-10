import express from 'express'
import webpack from 'webpack'
import webpackDevMiddleware from 'webpack-dev-middleware'

import configCreator from './webpack.config'
import { WebpackConfiguration } from 'webpack-cli'

const config = configCreator() as WebpackConfiguration
const app = express()
const compiler = webpack(config)

if (compiler !== null) {
  app.use(
    webpackDevMiddleware(compiler, {
      publicPath: config.output?.publicPath,
      writeToDisk: true
    })
  )

  app.get('/ws', (req, res) => {
    res.redirect('ws://localhost:9000/ws')
  })

  app.get(`${config.output?.publicPath}/:file`, (req, res, next) => {
    const file = req.params.file

    res.sendFile(file, { root: config.output?.path }, (err) => err && next(err))
  })

  app.get('/*splat', (req, res) => {
    res.sendFile('index.html', {
      root: config.output?.path
    })
  })

  app.listen(8080, () => {
    console.log('Listening on port 8080')
  })
}
