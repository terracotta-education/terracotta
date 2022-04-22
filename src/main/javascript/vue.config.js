const path = require("path");

module.exports = {
  outputDir: path.resolve(__dirname, "../resources/static/app"),
  indexPath: path.resolve(__dirname, "../resources/static/app/app.html"),
  publicPath: "/app/",
  assetsDir: "./",
  pages: {
    app: 'src/main.js',
    storageAccessCheck: 'src/storageAccessCheck.js',
    firstParty: 'src/firstPartyInteraction.js',
  },

  transpileDependencies: [
    'vuetify'
  ],
  chainWebpack: config => {
    config
      .module
      .rule('file-loader')
        .test(/\.(doc|docx|csv|xlsx|xls)$/)
        .use('file-loader')
          .loader('file-loader')
          .options({
            name: `[path][name].[ext]`
          })
      .end()
  }
}
