module.exports = function (config) {
  var isElectron = process.env.KARMA_ELECTRON === '1'
  var junitOutputDir = 'junit'

  config.set({
    browserDisconnectTimeout: 10000,
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
        require: __dirname + '/test/electron/test-main.js',
        browserWindowOptions: {
          webPreferences: {
            preload: __dirname + '/resources/preload.js',
            sandbox: false
          }
        }
      }
    },
    browsers: [isElectron ? 'CustomElectron' : 'ChromeHeadlessNoSandbox'],
    basePath: 'target',
    files: [isElectron ? 'karma-electron-test.js' : 'karma-test.js'],
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
