# Contributing

Thank you for your interest in participating in the project's development!
Please read the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md)
and the [Contributor License Agreement](CLA.md) first. If you plan to use LLMs
to contribute, make sure you also read our [AI and LLM policy](LLM_POLICY.md).

The project is written in [ClojureScript](https://clojurescript.org/) - a
compiler for [Clojure](https://clojure.org/) that targets JavaScript, and is
based on [re-frame](https://github.com/day8/re-frame/) - a framework for
building Modern Web Apps in ClojureScript. You should probably take a look at
their [exceptional documentation](https://day8.github.io/re-frame/re-frame/)
first.

## Style Guide

We try to follow the [Clojure Style Guide](https://guide.clojure.style/) as much
as possible. You are also advised to read an additional resource about
[how to name Clojure functions](https://stuartsierra.com/2016/01/09/how-to-name-clojure-functions)
by Stuart Sierra. In addition to the [idiomatic names](https://guide.clojure.style/#idiomatic-names),
we use the following conventions

```text
e           -> event
el, els     -> element, elements
attr, attrs -> attribute, attributes
prop, props -> property, properties
w, h        -> width, height
t           -> time
h, m, s, ms -> hours, minutes, seconds, milliseconds
```

## App structure

Main structure

<div class="highlight"><pre><code>src\
├── renderer\ -> <a href="https://www.electronjs.org/docs/latest/tutorial/process-model#the-renderer-process"
                 >Renderer Process</a>
├── electron\ -> <a href="https://www.electronjs.org/docs/latest/tutorial/process-model#the-main-process"
                 >Main Process</a> & <a href="https://www.electronjs.org/docs/latest/tutorial/process-model#preload-scripts"
                 >Preload script</a>
├── lang\     -> Translation files
└── worker\   -> <a href="https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API"
                 >Web Workers</a>
</code></pre></div>

We are trying to split our code under renderer into relatively independent
modules, following [re-frame's app structure suggestions](https://day8.github.io/re-frame/App-Structure/)
with some additions.

```text
module\
├── impl\           -> built-in multimethod implementations
├── core.cljs       -> entry point (will be evaluated on load)
├── db.cljs         -> schema, validation
├── views.cljs      -> reagent views
├── events.cljs     -> event handlers
├── subs.cljs       -> subscription handlers
├── handlers.cljs   -> helper functions for db transformations
├── effects.cljs    -> effect handlers
├── hierarchy.cljs  -> multimethod and hierarchy definitions
├── migrations.cljs -> data migrations
└── README.md       -> documentation
```

### Core ns

Core requires the rest of the namaspaces (subs, events, etc). We also register
the corresponding actions and action groups.

### Db ns

Db defines the [malli](https://github.com/metosin/malli) schemas of the module,
and the corresponding [validators](https://github.com/metosin/malli#validation),
[explainers](https://github.com/metosin/malli#humanized-error-messages), and
[transformers](https://github.com/metosin/malli#value-transformation).

### Events ns

Registers our re-frame events. Most events use functions from handlers to
transform our db.

### Subs ns

Registers our re-frame subscriptions. This ns is usually very thin.

### Handlers ns

Handlers contain pure functions that directly transform the db, or return a
value based on the db. Most functions under `handlers` take the db as their
first argument, so they can be easily composed using the thread-first macro `->`.
Functions without input args usually return a [transducer](https://clojure.org/reference/transducers).
If none of the above is true, the function probably belongs to a different
namespace.

### Effects ns

Registers all re-frame effects. Although we could use the events ns for this, we
prefer using dedicated ns to isolate all side effects and make stabbing easier on
tests.

### Hierarchy ns

Hierarchy defines the required multimethods in order to allow extending the app
on the fly, based on a dispatch value. In a way, it is our plugin interface.
When the hierarchy ns is available for a module, the `impl` directory contains
the build-in `defmethod` implementations for the multimethods.

## General re-frame recommendations

Avoid chaining events to create new ones. Always prefer composing transformation
functions. That is the whole purpose of `handlers` namespace. Re-frame events
should be treated as db transactions. All of the required operations should be
handled within the event.

Use interceptors sparingly. Although they look (and probably are) ingenious, it
is hard to write and reason with them. Doing things explicitly, is usually
easier to grasp and maintain.

Always use auto-qualified keywords (e.g. `::copy`) for subscriptions, events and
effects. You can use `as-alias` to require those namespaces without evaluating
the registrations multiple times.

When you are not sure if you should add new state to the app db, the answer is
usually yes, if the state is going to be used outside of the context of a single
component view. If it also needs to be persisted to local storage, it should be
serializable to json.

If you need to reuse the result of a subscription within an event, add reusable
functions to handlers, and use them in subs and events. If it's an expensive
calculation, you can write the result to the db within the event handler, or in
an interceptor. Using the recently introduced [flows](https://day8.github.io/re-frame/Flows/)
is also an option, but we haven't tested that feature yet.

## Spec

You can use the [schema explorer](https://repath.studio/schema-explorer/) to
examine the app schemas and their relationships.

We use [malli](https://github.com/metosin/malli) to describe the shape of our
app db and selectively validate incoming data (e.g. file loading). We also use
this spec to generate default values. Full db validation can be enabled on dev
mode.

[Function schemas](https://github.com/metosin/malli/blob/master/docs/function-schemas.md#defn-schemas)
are selectively applied to pure and critical namespaces, such as utils and
handlers. By default, function schemas are instrumented only during tests to
avoid performance overhead. However, runtime instrumentation can also be enabled
in the development environment (see `dev.cljs`).

## Upgrading dependencies

### NPM packages

Running `npm run upgrade` automatically upgrades `package.json` dependencies to
the latest versions. The script checks for new versions with 7 day minimum age
to reduce the risk of installing compromised packages. If just want to look at
the available upgrades without changing `package.json`, you can run
`npm outdated`.

After upgrading we need to check the release notes of the upgraded packages, to
make sure there is no breaking change, and to look for introduced features or
changes that can improve our application.

If a package introduced a lot of breaking changes, and requires special
handling, we can add it to the ignore list by passing
`-x 'ignored-package-name'` to `npm-check-updates`. In this case, we need to
also open a corresponding upgrade issue with its priority set to `high`, to keep
track of those packages and handle the upgrade as soon as possible.

When we are happy with the `package.json` changes, we need to manually run
`npm install` to also update our `package-lock.json`, and handle any package
resolution issues that may arise.

Before pushing those changes, we need to test all related functionality to
ensure that there are no regressions.

## Clojure packages

We can check for outdated Clojure dependencies by running `clojure -M:outdated`.
There is an automated action that runs this check once a week.

## Useful development shortcuts

```text
Ctrl+Shift+I -> Toggle devtools
Ctrl+Shift+X -> Toggle 10x
Ctrl+R       -> Reload app
```
