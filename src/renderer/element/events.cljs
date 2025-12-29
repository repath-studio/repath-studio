(ns renderer.element.events
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.document.events :as-alias document.events]
   [renderer.effects :as-alias effects]
   [renderer.element.db :as element.db]
   [renderer.element.effects :as-alias element.effects]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.menubar.views :as-alias menubar.views]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.extra :refer [partial-right]]))

(rf/reg-event-db
 ::select
 (fn [db [_ id additive]]
   (-> (element.handlers/toggle-selection db id additive)
       (history.handlers/finalize (if additive
                                    [::modify-selection "Modify selection"]
                                    [::select-element "Select element"])))))

(rf/reg-event-db
 ::select-ids
 (fn [db [_ ids]]
   (-> (partial-right element.handlers/assoc-prop :selected true)
       (reduce (element.handlers/deselect db) ids)
       (history.handlers/finalize [::select-elements "Select elements"]))))

(rf/reg-event-db
 ::toggle-prop
 (fn [db [_ id k explanation]]
   (-> (element.handlers/update-prop db id k not)
       (history.handlers/finalize explanation))))

(rf/reg-event-db
 ::set-label
 (fn [db [_ id v]]
   (-> (element.handlers/assoc-prop db id :label v)
       (history.handlers/finalize [::set-label "Set label"]))))

(rf/reg-event-db
 ::lock
 (fn [db]
   (-> (element.handlers/assoc-prop db :locked true)
       (history.handlers/finalize [::lock-selection "Lock selection"]))))

(rf/reg-event-db
 ::unlock
 (fn [db]
   (-> (element.handlers/assoc-prop db :locked false)
       (history.handlers/finalize [::unlock-selection "Unlock selection"]))))

(rf/reg-event-db
 ::set-attr
 (fn [db [_ k v]]
   (-> (element.handlers/set-attr db k v)
       (history.handlers/finalize [::set "Set %1"] [(name k)]))))

(rf/reg-event-db
 ::remove-attr
 (fn [db [_ k]]
   (-> (element.handlers/dissoc-attr db k)
       (history.handlers/finalize [::remove "Remove %1"] [(name k)]))))

(rf/reg-event-db
 ::update-attr
 (fn [db [_ k f & more]]
   (-> (apply partial-right element.handlers/update-attr k f more)
       (reduce db (element.handlers/selected-ids db))
       (history.handlers/finalize [::update "Update %1"] [(name k)]))))

(rf/reg-event-db
 ::preview-attr
 (fn [db [_ k v]]
   (element.handlers/set-attr db k v)))

(rf/reg-event-db
 ::delete
 (fn [db]
   (-> (element.handlers/delete db)
       (history.handlers/finalize [::delete-selection "Delete selection"]))))

(rf/reg-event-db
 ::deselect-all
 (fn [db]
   (-> (element.handlers/deselect db)
       (history.handlers/finalize [::deselect-all "Deselect all"]))))

(rf/reg-event-db
 ::select-all
 (fn [db]
   (-> (element.handlers/select-all db)
       (history.handlers/finalize [::select-all "Select all"]))))

(rf/reg-event-db
 ::select-same-tags
 (fn [db]
   (-> (element.handlers/select-same-tags db)
       (history.handlers/finalize [::select-same-tags "Select same tags"]))))

(rf/reg-event-db
 ::invert-selection
 (fn [db]
   (-> (element.handlers/invert-selection db)
       (history.handlers/finalize [::invert-selection "Invert selection"]))))

(rf/reg-event-db
 ::raise
 (fn [db]
   (-> (element.handlers/update-index db inc)
       (history.handlers/finalize [::raise-selection "Raise selection"]))))

(rf/reg-event-db
 ::lower
 (fn [db]
   (-> (element.handlers/update-index db dec)
       (history.handlers/finalize [::lower-selection "Lower selection"]))))

(rf/reg-event-db
 ::raise-to-top
 (fn [db]
   (-> (element.handlers/update-index db (fn [_i sibling-count]
                                           (dec sibling-count)))
       (history.handlers/finalize [::raise-selection-top
                                   "Raise selection to top"]))))

(rf/reg-event-db
 ::lower-to-bottom
 (fn [db]
   (-> (element.handlers/update-index db #(identity 0))
       (history.handlers/finalize [::lower-selection-bottom
                                   "Lower selection to bottom"]))))

(rf/reg-event-db
 ::align
 (fn [db [_ direction]]
   (-> (element.handlers/align db direction)
       (history.handlers/finalize [::update "Update %1"] [direction]))))

(rf/reg-event-db
 ::paste
 (fn [db]
   (-> (element.handlers/paste db)
       (history.handlers/finalize [::paste-selection "Paste selection"]))))

(rf/reg-event-db
 ::paste-in-place
 (fn [db]
   (-> (element.handlers/paste-in-place db)
       (history.handlers/finalize [::paste-selection-in-place
                                   "Paste selection in place"]))))

(rf/reg-event-db
 ::paste-styles
 (fn [db]
   (-> (element.handlers/paste-styles db)
       (history.handlers/finalize [::paste-styles-to-selection
                                   "Paste styles to selection"]))))

(rf/reg-event-db
 ::duplicate
 (fn [db]
   (-> (element.handlers/duplicate db)
       (history.handlers/finalize [::duplicate-selection
                                   "Duplicate selection"]))))

(rf/reg-event-db
 ::translate
 (fn [db [_ offset]]
   (-> (element.handlers/translate db offset)
       (history.handlers/finalize [::move-selection "Move selection"]))))

(rf/reg-event-db
 ::place
 (fn [db [_ position]]
   (-> (element.handlers/place db position)
       (history.handlers/finalize [::place-selection "Place selection"]))))

(rf/reg-event-db
 ::scale
 (fn [db [_ ratio]]
   (let [pivot-point (-> db element.handlers/bbox utils.bounds/center)]
     (-> (element.handlers/scale db ratio pivot-point false)
         (history.handlers/finalize [::scale-selection "Scale selection"])))))

(rf/reg-event-fx
 ::->path
 (fn [{:keys [db]}]
   {::element.effects/->path
    {:data (element.handlers/selected db)
     :on-success [::finalize->path]
     :on-error [::app.events/toast-error]}}))

(rf/reg-event-db
 ::finalize->path
 (fn [db [_ elements]]
   (-> (reduce element.handlers/swap db elements)
       (history.handlers/finalize [::convert-selection-path
                                   "Convert selection to path"]))))

(rf/reg-event-fx
 ::stroke->path
 (fn [{:keys [db]}]
   {::element.effects/->path
    {:data (element.handlers/selected db)
     :on-success [::finalize-stroke->path]
     :on-error [::app.events/toast-error]}}))

(rf/reg-event-db
 ::finalize-stroke->path
 (fn [db [_ elements]]
   (-> (reduce element.handlers/swap db elements)
       (element.handlers/stroke->path)
       (history.handlers/finalize [::convert-selection-stroke-path
                                   "Convert selection's stroke to path"]))))

(rf/reg-event-fx
 ::boolean-operation
 (fn [{:keys [db]} [_ operation]]
   (when (seq (rest (element.handlers/selected db)))
     {::element.effects/->path
      {:data (element.handlers/selected db)
       :on-success [::finalize-boolean-operation operation]
       :on-error [::app.events/toast-error]}})))

(rf/reg-event-db
 ::finalize-boolean-operation
 (fn [db [_ operation elements]]
   (-> (reduce element.handlers/swap db elements)
       (element.handlers/boolean-operation operation)
       (history.handlers/finalize (case operation
                                    :unite [::menubar.views/unite]
                                    :intersect [::menubar.views/intersect]
                                    :subtract [::menubar.views/subtract]
                                    :exclude [::menubar.views/exclude]
                                    :divide [::menubar.views/divide])))))

(rf/reg-event-db
 ::add
 (fn [db [_ el]]
   (-> (element.handlers/add db el)
       (history.handlers/finalize [::create "Create %1"] [(name (:tag el))]))))

(rf/reg-event-db
 ::import-svg
 (fn [db [_ data]]
   (-> (element.handlers/import-svg db data)
       (history.handlers/finalize [::import-svg "Import svg"]))))

(rf/reg-event-db
 ::animate
 (fn [db [_ tag attrs]]
   (-> (element.handlers/animate db tag attrs)
       (history.handlers/finalize (case tag
                                    :animate
                                    [::menubar.views/animate]

                                    :animateTransform
                                    [::menubar.views/animate-transform]

                                    :animateMotion
                                    [::menubar.views/animate-motion])))))

(rf/reg-event-db
 ::set-parent
 (fn [db [_ id parent-id]]
   (-> (element.handlers/set-parent db id parent-id)
       (history.handlers/finalize [::set-parent "Set parent"]))))

(rf/reg-event-db
 ::group
 (fn [db]
   (-> (element.handlers/group db)
       (history.handlers/finalize [::group-selection "Group selection"]))))

(rf/reg-event-db
 ::ungroup
 (fn [db]
   (-> (element.handlers/ungroup db)
       (history.handlers/finalize [::ungroup-selection "Ungroup selection"]))))

(rf/reg-event-db
 ::manipulate-path
 (fn [db [_ action]]
   (-> (element.handlers/manipulate-path db action)
       (history.handlers/finalize (case action
                                    :simplify
                                    [::menubar.views/boolean-simplify]

                                    :smooth
                                    [::menubar.views/boolean-smooth]

                                    :flatten
                                    [::menubar.views/boolean-flatten]

                                    :reverse
                                    [::menubar.views/boolean-reverse])))))

(rf/reg-event-fx
 ::copy
 (fn [{:keys [db]} _]
   (let [els (element.handlers/top-selected-sorted db)]
     {:db (element.handlers/copy db)
      :fx [(when (seq els)
             [::effects/clipboard-write
              {:data (utils.element/->svg els)
               :on-error [::app.events/toast-error]}])]})))

(rf/reg-event-fx
 ::cut
 (fn [{:keys [db]} _]
   (let [els (element.handlers/top-selected-sorted db)]
     {:db (-> (element.handlers/copy db)
              (element.handlers/delete)
              (history.handlers/finalize [::cut-selection "Cut selection"]))
      :fx [(when (seq els)
             [::effects/clipboard-write
              {:data (utils.element/->svg els)
               :on-error [::app.events/toast-error]}])]})))

(rf/reg-event-fx
 ::trace
 (fn [{:keys [db]} [_]]
   (let [images (element.handlers/filter-by-tag db :image)]
     {::element.effects/trace {:data images
                               :on-success [::create-traced-image]}})))

(rf/reg-event-db
 ::create-traced-image
 (fn [db [_ data]]
   (-> (element.handlers/import-svg db data)
       (history.handlers/finalize [::trace-image "Trace image"]))))

(rf/reg-event-fx
 ::import-file
 (fn [_ [_ ^js/FileSystemFileHandle file-handle ^js/File file position]]
   (when-let [file-type (.-type file)]
     (cond
       (= file-type "image/svg+xml")
       {::effects/file-read-as
        [file :text {"load" {:formatter (fn [data]
                                          {:svg data
                                           :label (.-name file)
                                           :position position})
                             :on-fire [::import-svg]}
                     "error" {:on-fire [::app.events/toast-error]}}]}

       (contains? element.db/image-mime-types file-type)
       {::element.effects/import-image
        {:file file
         :position position
         :on-success [::add]
         :on-error [::app.events/toast-error]}}

       :else
       (let [extension (last (string/split (.-name file) "."))]
         (when (= extension "rps")
           {:dispatch [::document.events/file-read nil file-handle file]}))))))
