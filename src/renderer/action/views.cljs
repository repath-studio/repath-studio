(ns renderer.action.views
  (:require
   [re-frame.core :as rf]
   [renderer.action.subs :as action.subs]
   [renderer.i18n.views :as i18n.views]))

(defn disabled?
  "Returns the disabled html attribute state.
   An action is disabled if it has an :enabled subscription that returns false,
   or if it has an :enabled boolean value of false."
  [{:keys [enabled]}]
  (cond
    (false? enabled)
    true

    (vector? enabled)
    (some-> enabled
            rf/subscribe
            deref
            not)

    :else
    nil))

(defn available?
  "An action is active if it doesn't have an :available subscription,
   or if it has an :available subscription that returns true."
  [action]
  (if-not (:available action)
    true
    (-> action
        :available
        rf/subscribe
        deref)))

(defn checked?
  "Returns the checked html attribute state.
   An action is checked if it has a :checked subscription that returns true."
  [action]
  (some-> action
          :active
          rf/subscribe
          deref))

(defn dispatch
  [action]
  (fn [_e]
    (some-> action
            :event
            rf/dispatch)))

(defn label
  [action]
  (some-> action
          :label
          i18n.views/t))

(defn entity
  [id]
  (when-let [action @(rf/subscribe [::action.subs/entity id])]
    (when (available? action)
      action)))
