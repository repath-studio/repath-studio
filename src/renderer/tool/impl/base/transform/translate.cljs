(ns renderer.tool.impl.base.transform.translate
  (:require [clojure.core.matrix :as matrix]
            [malli.core :as m]
            [renderer.app.db :refer [App]]
            [renderer.db :refer [Orientation Vec2]]
            [renderer.element.db :refer [Element]]
            [renderer.element.handlers :as element.handlers]
            [renderer.history.handlers :as history.handlers]
            [renderer.i18n.views :as i18n.views]
            [renderer.input.db :refer [PointerEvent]]
            [renderer.snap.handlers :as snap.handlers]
            [renderer.tool.handlers :as tool.handlers]
            [renderer.tool.hierarchy :as tool.hierarchy]
            [renderer.tool.impl.base.transform.select :as transform.select]
            [renderer.utils.element :as utils.element]
            [renderer.utils.extra :refer [partial-right]]
            [renderer.views :as views]))

(defmethod tool.hierarchy/help [:transform :translate]
  []
  (i18n.views/t [::translate
                 [:div "Hold %1 to restrict direction, and %2 to clone."]]
                [[views/kbd "Ctrl"]
                 [views/kbd "Alt"]]))

(m/=> start-point [:-> Element Vec2])
(defn start-point
  [el]
  (into [] (take 2) (:bbox el)))

(m/=> swap-parent [:-> App uuid? Element Element App])
(defn swap-parent
  [db id hovered-svg container-el]
  (cond-> db
    :always
    (element.handlers/set-parent (:id hovered-svg))

    (:bbox container-el)
    (element.handlers/translate id (start-point container-el))

    (:bbox hovered-svg)
    (element.handlers/translate id (matrix/mul (start-point hovered-svg) -1))))

(m/=> translate-el [:-> App uuid? map? App])
(defn translate-el
  [db id {:keys [offset hovered-svg auto-parent]}]
  (let [el (element.handlers/entity db id)
        container-el (element.handlers/parent-container db id)
        parent-el (element.handlers/parent db id)]
    (cond-> db
      :always
      (element.handlers/translate id offset)

      (and auto-parent
           (not= (:id parent-el) (:id hovered-svg))
           (not (utils.element/svg? el)))
      (swap-parent id hovered-svg container-el))))

(m/=> translate [:-> App Vec2 [:maybe Orientation] App])
(defn translate
  [db offset axis]
  (let [[offset-x offset-y] offset
        hovered-svg (element.handlers/hovered-svg db)
        selected-els (element.handlers/selected db)
        auto-parent? (and (contains? #{:translate :clone} (:state db))
                          (seq selected-els)
                          (not (utils.element/top-level? (first selected-els)))
                          (empty? (rest selected-els)))
        offset (case axis
                 :vertical [offset-x 0]
                 :horizontal [0 offset-y]
                 offset)]
    (->> (element.handlers/top-ancestor-ids db)
         (reduce (partial-right translate-el {:offset offset
                                              :hovered-svg hovered-svg
                                              :auto-parent auto-parent?}) db))))

(m/=> on-drag [:-> App Vec2 Orientation PointerEvent App])
(defn on-drag
  [db delta axis e]
  (let [{:keys [shift-key]} e
        selected-elements (element.handlers/selected db)
        locked? (every? :locked selected-elements)]
    (-> db
        (history.handlers/reset-state)
        (transform.select/select-element shift-key)
        (translate delta axis)
        (snap.handlers/snap-with translate axis)
        (tool.handlers/set-cursor (if locked? "not-allowed" "move")))))
