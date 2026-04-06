(ns renderer.tool.impl.element.text
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.key :as utils.key]))

(tool.hierarchy/derive-tool :text ::tool.hierarchy/element)

(defmethod tool.hierarchy/help [:text :idle]
  []
  (i18n.views/t [::help "Click to start typing."]))

(defmethod tool.hierarchy/on-activate :text
  [db]
  (tool.handlers/set-cursor db "text"))

(defmethod tool.hierarchy/on-pointer-up :text
  [db _e]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        el {:type :element
            :tag :text
            :attrs {:x offset-x
                    :y offset-y}}]
    (-> db
        (element.handlers/deselect)
        (element.handlers/add el)
        (tool.handlers/set-state :type)
        (tool.handlers/activate :edit))))

(defmethod tool.hierarchy/on-drag-end :text
  [db e]
  (tool.hierarchy/on-pointer-up db e))

(rf/dispatch [::action.events/register-action
              {:id :tool/text
               :label [::label "Text"]
               :icon "text"
               :event [::tool.events/activate :text]
               :active [::tool.subs/active? :text]
               :shortcuts [{:keyCode (utils.key/codes "T")}]}])
