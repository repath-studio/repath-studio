(ns renderer.tool.impl.element.text
  (:require
   [renderer.element.handlers :as element.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(defmethod tool.hierarchy/properties :text
  []
  {:icon "text"
   :label [::label "Text"]})

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
