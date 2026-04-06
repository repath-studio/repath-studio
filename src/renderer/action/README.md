# Action module

This module is an abstraction that describes user facing actions, in order to
connect the following concepts

- event
- icon
- label
- shortcuts
- state subscriptions (enabled, available, active)

Action groups are sequences of actions that can be reused in various places
through our UI. They are also extensible, so if a component list is populated
using an action group, it is will also be extensible.
