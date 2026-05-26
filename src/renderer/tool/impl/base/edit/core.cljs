(ns renderer.tool.impl.base.edit.core
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.hierarchy :as hierarchy]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.base.edit.edit]
   [renderer.tool.impl.base.edit.idle]
   [renderer.tool.impl.base.edit.select]
   [renderer.tool.impl.base.edit.type]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.element :as utils.element]
   [renderer.utils.key :as utils.key]
   [renderer.utils.svg :as utils.svg]))

(hierarchy/derive! ::edit ::tool.hierarchy/tool)

(defmethod tool.hierarchy/on-activate ::edit
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in [:documents (:active-document db) :selected-handles] #{})))

(defmethod tool.hierarchy/render ::edit
  []
  (let [selected-elements @(rf/subscribe [::element.subs/selected])
        select-box @(rf/subscribe [::tool.subs/select-box])]
    (->> selected-elements
         (map (fn [el]
                [:g
                 [element.hierarchy/render-edit el]
                 (when-let [pos (element.hierarchy/centroid el)]
                   (let [offset (utils.element/offset el)
                         pos (matrix/add offset pos)]
                     [utils.svg/dot pos
                      [:title (i18n.views/t [::centroid "Centroid"])]]))]))
         (into [:g [element.hierarchy/render select-box]]))))

(rf/dispatch [::action.events/register-action
              {:id :tool/edit
               :label [::label "Edit"]
               :icon "edit"
               :event [::tool.events/activate ::edit]
               :active [::tool.subs/active? ::edit]
               :shortcuts [{:keyCode (utils.key/codes "E")}]}])
