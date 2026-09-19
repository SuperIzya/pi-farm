import express from 'express'
import webpack from 'webpack'
import webpackDevMiddleware from 'webpack-dev-middleware'
import { createProxyMiddleware} from 'http-proxy-middleware'

import configCreator from './webpack.config'
import {Configuration as WebpackConfiguration} from 'webpack'

const config = configCreator() as WebpackConfiguration
const app = express()
const compiler = webpack(config)
const server = 'http://localhost:9000'

const allProxy = createProxyMiddleware({
    target: `${server}`,
    changeOrigin: true,
    logger: console,
    pathFilter: ['/api', '/images']
})

const wsProxy = createProxyMiddleware({
    target: server,
    changeOrigin: true,
    ws: true,
    logger: console,
})

if (compiler !== null) {    
    app.use(
        webpackDevMiddleware(compiler, {
            publicPath: config.output?.publicPath,
            writeToDisk: true
        })
    )


    app.use(allProxy)

    app.get(`${config.output?.publicPath}/:file`, (req, res, next) => {
        const file = req.params.file

        res.sendFile(file, {root: config.output?.path}, (err) => err && next(err))
    })

    app.get('/*splat', (req, res) => {
        res.sendFile('index.html', {
            root: config.output?.path
        })
    })

    app.listen(8080).on('upgrade', wsProxy.upgrade)
        
}
