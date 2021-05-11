const path = require("path");

module.exports = {
    outputDir: path.resolve(__dirname, "../resources/static/app"),
    indexPath: path.resolve(__dirname, "../resources/static/app/app.html"),
    publicPath: "/app/",
    assetsDir: "./",
    filenameHashing: false,

    transpileDependencies: [
      'vuetify'
    ]
}
