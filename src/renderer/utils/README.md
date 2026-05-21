# A collection of utility namespaces

Reusable helper functions that don't take the whole app db as their first
argument, usually belong here. The opposite is also true. A function that
expects the db as its input, probably needs to live under the corresponding
`handlers.cljs` file.
