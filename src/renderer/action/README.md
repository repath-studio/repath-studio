# Action module

## Action registry

This module is an abstraction that describes user facing actions, in order to
connect the following concepts

- event
- icon
- label
- shortcuts
- state subscriptions (enabled, available, active)

## Action group registry

Action groups are sequences of actions that can be reused in various places
through our UI. They are also extensible, so if a component list is populated
using an action group, it is will also be extensible.

## Icons

Icons are optional. We should not try to cover all actions or groups. Icons
should be used on primary operations or groups, that usually need to be
presented as icon buttons without text. We should also avoid assigning the same
icon on multiple actions, unless it really makes sense to do so. As a general
rule, don't try to set an icon, just for the sake of having one.
