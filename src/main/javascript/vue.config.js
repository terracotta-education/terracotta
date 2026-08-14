const path = require("path");

module.exports = {
  // Lint runs as its own "yarn lint" step; running it again inline via eslint-loader on every
  // build adds real time for no benefit, since a lint failure here wouldn't fail the build anyway.
  lintOnSave: false,
  outputDir: path.resolve(__dirname, "../resources/static/app"),
  indexPath: path.resolve(__dirname, "../resources/static/app/app.html"),
  publicPath: "/app/",
  assetsDir: "./",
  pages: {
    app: "src/main.js",
    storageAccessRequest: "src/storageAccessRequest.js",
    firstParty: "src/firstPartyInteraction.js",
    deepLink: "src/deepLink.js"
  },
  transpileDependencies: [
    "vuetify"
  ],
  chainWebpack: config => {
    config
      .module
      .rule("file-loader")
        .test(/\.(doc|docx|csv|xlsx|xls)$/)
        .use("file-loader")
          .loader("file-loader")
          .options({
            name: `[path][name].[ext]`
          })
      .end()
  }
}
