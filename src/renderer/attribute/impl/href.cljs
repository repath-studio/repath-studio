(ns renderer.attribute.impl.href
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/href"
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.db :as element.db]
   [renderer.element.effects :as-alias element.effects]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as-alias element.hierarchy]
   [renderer.events :as-alias events]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/description [::element.hierarchy/element :href]
  []
  [::description
   "The href attribute defines a link to a resource as a reference URL.
    The exact meaning of that link depends on the context of each element
    using it."])

(defmethod attribute.hierarchy/form-element [:image :href]
  [_ k v {:keys [disabled]}]
  (let [state-default? (= @(rf/subscribe [::tool.subs/state]) :idle)
        data-url? (and v (string/starts-with? v "data:"))]
    [:div.flex.gap-px.w-full
     [attribute.views/form-input k (if data-url? "data-url" v)
      {:placeholder (when-not v "multiple")
       :disabled (or disabled
                     data-url?
                     (not v)
                     (not state-default?))}]
     [:button.form-control-button
      {:title (i18n.views/t [::select-file "Select file"])
       :disabled disabled
       :on-click #(rf/dispatch
                   [::events/file-open
                    {:options {:startIn "pictures"
                               :types [{:accept element.db/image-mime-types}]}
                     :on-success [::success]
                     :on-error [::app.events/toast-error]}])}
      [views/icon "folder"]]]))

(rf/reg-event-fx
 ::success
 (fn [_ [_ _file-handle file]]
   {::element.effects/import-image
    {:file file
     :on-success [::update-attrs]
     :on-error [::app.events/toast-error]}}))

(rf/reg-event-fx
 ::update-attrs
 (fn [{:keys [db]} [_ el]]
   (let [attrs (select-keys (:attrs el) [:href :width :height])]
     {:db (tool.handlers/activate db :transform)
      :dispatch-n (mapv (fn [[k v]] [::element.events/set-attr k v]) attrs)})))
