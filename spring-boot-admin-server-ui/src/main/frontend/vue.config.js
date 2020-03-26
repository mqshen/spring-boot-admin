module.exports = {
    devServer: {
      port: 8082,
      proxy: {
        '^/api': {
          target: 'http://localhost:8089',
          changeOrigin: true
        },
        '^/instances': {
          target: 'http://localhost:8089',
          changeOrigin: true
        }
      }
    },
    runtimeCompiler: true
}
