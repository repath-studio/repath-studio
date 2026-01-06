(ns renderer.document.subs
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.handlers :as document.handlers]
   [renderer.timeline.subs :as-alias timeline.subs]
   [renderer.window.subs :as-alias window.subs]))

(rf/reg-sub
 ::active-id
 :-> :active-document)

(rf/reg-sub
 ::documents
 :-> :documents)

(rf/reg-sub
 ::entities
 :<- [::documents]
 vals)

(rf/reg-sub
 ::tabs
 :-> :document-tabs)

(rf/reg-sub
 ::recent
 (fn [db _]
   (-> db :recent reverse)))

(rf/reg-sub
 ::entities?
 :<- [::entities]
 (comp boolean seq))

(rf/reg-sub
 ::recent?
 :<- [::recent]
 (comp boolean seq))

(rf/reg-sub
 ::active
 :<- [::documents]
 :<- [::active-id]
 (fn [[documents active-document] _]
   (some->> active-document
            (get documents))))

(rf/reg-sub
 ::active?
 :<- [::active-id]
 (fn [active-id [_ id]]
   (= active-id id)))

(rf/reg-sub
 ::entity
 :<- [::documents]
 (fn [documents [_ k]]
   (get documents k)))

(rf/reg-sub
 ::zoom
 :<- [::active]
 :-> :zoom)

(rf/reg-sub
 ::rotate
 :<- [::active]
 :-> :rotate)

(rf/reg-sub
 ::attrs
 :<- [::active]
 :-> :attrs)

(rf/reg-sub
 ::fill
 :<- [::attrs]
 :-> :fill)

(rf/reg-sub
 ::stroke
 :<- [::attrs]
 :-> :stroke)

(rf/reg-sub
 ::pan
 :<- [::active]
 :-> :pan)

(rf/reg-sub
 ::title
 :<- [::active]
 :-> :title)

(rf/reg-sub
 ::path
 :<- [::active]
 :-> :path)

(rf/reg-sub
 ::title-bar
 :<- [::title]
 :<- [::path]
 :<- [::active]
 :<- [::active-saved?]
 (fn [[title path active saved] _]
   (let [title (or path title)]
     (str (when (and active (not saved)) "• ")
          (some-> title (str " - "))
          config/app-name))))

(rf/reg-sub
 ::elements
 :<- [::active]
 :-> :elements)

(rf/reg-sub
 ::hovered-ids
 :<- [::active]
 :-> :hovered-ids)

(rf/reg-sub
 ::collapsed-ids
 :<- [::active]
 :-> :collapsed-ids)

(rf/reg-sub
 ::ignored-ids
 :<- [::active]
 :-> :ignored-ids)

(rf/reg-sub
 ::saved-history-index
 :<- [::active]
 :-> :saved-history-index)

(rf/reg-sub
 ::preview-label
 :<- [::active]
 :-> :preview-label)

(rf/reg-sub
 ::read-only?
 :<- [::preview-label]
 :<- [::timeline.subs/time]
 (fn [[preview-label current-time] _]
   (or preview-label
       (pos? current-time))))

(rf/reg-sub
 ::saved?
 (fn [db [_ id]]
   (document.handlers/saved? db id)))

(rf/reg-sub
 ::active-saved?
 (fn [{:keys [active-document]
       :as db} [_]]
   (some->> active-document
            (document.handlers/saved? db))))
(rf/reg-sub
 ::saveable?
 :<- [::entities?]
 :<- [::active-saved?]
 (fn [[entities? active-saved?] [_]]
   (and entities?
        (not active-saved?))))

(rf/reg-sub
 ::handle-size
 :<- [::zoom]
 :<- [::window.subs/md?]
 :<- [::app.subs/supported-feature? :touch]
 (fn [[zoom md? touch?] [_]]
   (let [base-size 13]
     (cond-> (/ base-size zoom)
       (and touch? (not md?))
       (* 1.8)))))
