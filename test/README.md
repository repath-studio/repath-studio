# Testing

We are using [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html#_testing)
that provides targets to make building tests easier.

## Running the tests

Browser tests are watched and served on dev at [`http://localhost:8081/`](http://localhost:8081/).
You can also individually run the tests by executing

```bash
npm run test
```

See [`shadow-cljs.edn`](../shadow-cljs.edn) and [`karma.conf.js`](../karma.conf.js)
for details.

## Writing tests

The testing environment is initialized on [`core-test`](core_test.cljs).
Function schemas are instrumented, and full db validation is enabled. The data
flows through our functions and the db is constantly validated to verify its
shape. Simple utilities can be tested using `cljs.test`. Please also refer to
the [official clojurescript testing guide](https://clojurescript.org/tools/testing).

We use [re-frame-test](https://github.com/day8/re-frame-test) that provides
utilities for testing re-frame applications. Those are not integration tests,
but they should be enough if we avoid computations on views. We should also
avoid requiring effects on tests. We register the [effects](https://day8.github.io/re-frame/Effects/)
and the [coeffects](https://day8.github.io/re-frame/Coeffects/)
that we need on [`fixtures`](fixtures.cljs) to make them deterministic. Please
also read the [testing guide of re-frame](https://github.com/day8/re-frame/blob/master/docs/Testing.md).
In general, we dispatch events, and then test the result of subscriptions.
We don't test view functions for now.

## CI/CD

The test are automatically run on pull requests, and every time we push changes
to our `main` branch or release a tag. If the tests fail, the web application is
not going to be updated, and the desktop binaries are not going to be released.
