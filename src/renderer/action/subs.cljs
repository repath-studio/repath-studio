(ns renderer.action.subs
  (:require
   [re-frame.core :as rf]
   [renderer.action.handlers :as action.handlers]
   [renderer.app.subs :as-alias app.subs]))

(rf/reg-sub
 ::actions
 :-> :actions)

(rf/reg-sub
 ::action
 :<- [::actions]
 (fn [actions [_ id]]
   (if (= id :separator)
     {:type :separator}
     (get actions id))))

(rf/reg-sub
 ::key-bindings
 :-> :key-bindings)

(rf/reg-sub
 ::action-shortcuts
 :<- [::actions]
 :<- [::key-bindings]
 :<- [::app.subs/web-platform]
 (fn [[actions key-bindings platform] [_ id]]
   (or (get key-bindings id)
       (set (action.handlers/shortcuts-for
             (get-in actions [id :shortcuts])
             platform)))))

(rf/reg-sub
 ::default-shortcut?
 :<- [::actions]
 :<- [::app.subs/web-platform]
 (fn [[actions platform] [_ id shortcut]]
   (boolean (some #{shortcut}
                  (action.handlers/shortcuts-for
                   (get-in actions [id :shortcuts])
                   platform)))))

(rf/reg-sub
 ::conflicting-action
 :<- [::actions]
 :<- [::key-bindings]
 :<- [::app.subs/web-platform]
 (fn [[actions key-bindings platform] [_ shortcut]]
   (when shortcut
     (some (fn [action]
             (when (some #{shortcut}
                         (or (get key-bindings (:id action))
                             (set (action.handlers/shortcuts-for
                                   (:shortcuts action)
                                   platform))))
               action))
           (vals actions)))))

(rf/reg-sub
 ::action-groups
 :-> :action-groups)

(rf/reg-sub
 ::groupless-actions
 :<- [::actions]
 :<- [::action-groups]
 (fn [[actions action-groups] _]
   (let [grouped-ids (mapcat :actions (vals action-groups))]
     (apply dissoc actions grouped-ids))))

(rf/reg-sub
 ::action-group
 :<- [::action-groups]
 :=> get)
