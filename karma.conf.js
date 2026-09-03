module.exports = function (config) {
  var junitOutputDir = process.env.CIRCLE_TEST_REPORTS || "target/junit"

  config.set({
    browserDisconnectTimeout: 5000,
    customLaunchers: {
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: [
          '--disable-gpu',
          '--no-sandbox'
        ]
      },
      CustomElectron: {
        base: 'Electron',
        require: __dirname + '/resources/main.js',
        browserWindowOptions: {
          webPreferences: {
            preload: __dirname + '/resources/preload.js',
            sandbox: false
          }
        }
      }
    },
    browsers: ['ChromeHeadlessNoSandbox', 'CustomElectron'],
    basePath: 'target',
    files: ['karma-test.js'],
    frameworks: ['cljs-test'],
    plugins: [
      'karma-electron',
      'karma-cljs-test',
      'karma-chrome-launcher',
      'karma-junit-reporter'
    ],
    colors: true,
    logLevel: config.LOG_INFO,
    client: {
      args: ['shadow.test.karma.init'],
      useIframe: false
    },

    // the default configuration
    junitReporter: {
      outputDir: junitOutputDir + '/karma', // results will be saved as outputDir/browserName.xml
      outputFile: undefined, // if included, results will be saved as outputDir/browserName/outputFile
      suite: '' // suite will become the package name attribute in xml testsuite element
    }
  })
}
