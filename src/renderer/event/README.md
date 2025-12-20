# Event Module

The event module provides a layer of abstraction for handling canvas user
interactions, including pointer/keyboard/wheel events and drag-and-drop
operations.

## Flow

Browser events are captured by implementation handlers. The default event
behavior is prevented and propagation is stopped. Native events are converted
to Clojure data structures, and used to dispatch synchronous re-frame events.
Core business logic processes the events and updates the application state.
Events are then delegated to the active tool (see `renderer.tool.hierarchy`).
Tools implement their own event handlers for those events, like `on-drag-start`.
