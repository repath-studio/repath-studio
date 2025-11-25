# Contributing

Thank you for your interest in actively participating in the project's development!
Please read the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md)
and the [Contributor License Agreement](CLA.md) first.

The project is written in [ClojureScript](https://clojurescript.org/) - a
compiler for [Clojure](https://clojure.org/) that targets JavaScript, and is
based on [re-frame](https://github.com/day8/re-frame/) - a framework for
building Modern Web Apps in ClojureScript. You should probably take a look at
their [exceptional documentation](https://day8.github.io/re-frame/re-frame/)
first.

## Style Guide

We try to follow the [Clojure Style Guide](https://guide.clojure.style/) as much
as possible.

An additional resource about [how to name Clojure functions](https://stuartsierra.com/2016/01/09/how-to-name-clojure-functions)
by Stuart Sierra.

In addition to the [idiomatic names](https://guide.clojure.style/#idiomatic-names),
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
with some minor additions.

```text
module\
├── core.cljs      -> entry point
├── db.cljs        -> schema, validation
├── views.cljs     -> reagent views
├── events.cljs    -> event handlers
├── subs.cljs      -> subscription handlers
├── handlers.cljs  -> helper functions for db transformations
├── effects.cljs   -> effect handlers
├── hierarchy.cljs -> multimethods and hierarchies
├── styles.css     -> styles
└── README.md      -> documentation
```

## Re-frame recommendations

Avoid chaining events to create new ones. Always prefer composing pure functions
that directly transform the db. That is the whole purpose of `handlers`
namespace. Most functions under `handlers` take the db as their first argument,
so they can be easily composed using the thread-first macro `->`.

Use interceptors sparingly. Although they look (and probably are) ingenious, it
is hard to write and reason with them. Doing things explicitly, is usually
easier to grasp and maintain.

Always use auto-qualified keywords (e.g. `::copy`) for subscriptions, events and
effects. You can use `as-alias` to require those namespaces without evaluating
the registrations multiple times.

## Spec

We use [malli](https://github.com/metosin/malli) to describe the shape of our
app db and selectively validate incoming data (e.g. file loading). We also use
this spec to generate default values. Full db validation is enabled on dev mode.

[Function schemas](https://github.com/metosin/malli/blob/master/docs/function-schemas.md#defn-schemas)
are selectively applied to pure and critical namespaces, such as utils and
handlers. By default, function schemas are instrumented only during tests to
avoid performance overhead. However, runtime instrumentation can also be enabled
in the development environment (see `dev.cljs`).

## Useful development shortcuts

```text
Ctrl+Shift+I -> Toggle devtools
Ctrl+Shift+X -> Toggle 10x
Ctrl+R       -> Reload app
```
