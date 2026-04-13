(ns renderer.element.events
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.document.events :as-alias document.events]
   [renderer.effects :as-alias effects]
   [renderer.element.core :as-alias element.core]
   [renderer.element.db :as element.db]
   [renderer.element.effects :as-alias element.effects]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.extra :refer [partial-right]]))

(rf/reg-event-fx
 ::select
 (fn [{:keys [db now]} [_ id additive]]
   {:db (-> (element.handlers/toggle-selection db id additive)
            (history.handlers/finalize now (if additive
                                             [::modify-selection
                                              "Modify selection"]
                                             [::select-element
                                              "Select element"])))}))

(rf/reg-event-fx
 ::select-ids
 (fn [{:keys [db now]} [_ ids]]
   {:db (-> (partial-right element.handlers/assoc-prop :selected true)
            (reduce (element.handlers/deselect db) ids)
            (history.handlers/finalize now [::select-elements
                                            "Select elements"]))}))

(rf/reg-event-fx
 ::toggle-prop
 (fn [{:keys [db now]} [_ id k explanation]]
   {:db (-> (element.handlers/update-prop db id k not)
            (history.handlers/finalize now explanation))}))

(rf/reg-event-fx
 ::set-label
 (fn [{:keys [db now]} [_ id v]]
   {:db (-> (element.handlers/assoc-prop db id :label v)
            (history.handlers/finalize now [::set-label "Set label"]))}))

(rf/reg-event-fx
 ::lock
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/assoc-prop db :locked true)
            (history.handlers/finalize now [::lock-selection
                                            "Lock selection"]))}))

(rf/reg-event-fx
 ::unlock
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/assoc-prop db :locked false)
            (history.handlers/finalize now [::unlock-selection
                                            "Unlock selection"]))}))

(rf/reg-event-fx
 ::set-attr
 (fn [{:keys [db now]} [_ k v]]
   {:db (-> (element.handlers/set-attr db k v)
            (history.handlers/finalize now [::set "Set %1"] [(name k)]))}))

(rf/reg-event-fx
 ::remove-attr
 (fn [{:keys [db now]} [_ k]]
   {:db (-> (element.handlers/dissoc-attr db k)
            (history.handlers/finalize now
                                       [::remove "Remove %1"]
                                       [(name k)]))}))

(rf/reg-event-fx
 ::update-attr
 (fn [{:keys [db now]} [_ k f & more]]
   {:db (-> (apply partial-right element.handlers/update-attr k f more)
            (reduce db (element.handlers/selected-ids db))
            (history.handlers/finalize now
                                       [::update "Update %1"]
                                       [(name k)]))}))

(rf/reg-event-db
 ::preview-attr
 (fn [db [_ k v]]
   (element.handlers/set-attr db k v)))

(rf/reg-event-fx
 ::delete
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/delete db)
            (history.handlers/finalize now [::delete-selection
                                            "Delete selection"]))}))

(rf/reg-event-fx
 ::deselect-all
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/deselect db)
            (history.handlers/finalize now [::deselect-all "Deselect all"]))}))

(rf/reg-event-fx
 ::select-all
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/select-all db)
            (history.handlers/finalize now [::select-all "Select all"]))}))

(rf/reg-event-fx
 ::select-same-tags
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/select-same-tags db)
            (history.handlers/finalize now [::select-same-tags
                                            "Select same tags"]))}))

(rf/reg-event-fx
 ::invert-selection
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/invert-selection db)
            (history.handlers/finalize now [::invert-selection
                                            "Invert selection"]))}))

(rf/reg-event-fx
 ::raise
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/update-index db inc)
            (history.handlers/finalize now [::raise-selection
                                            "Raise selection"]))}))

(rf/reg-event-fx
 ::lower
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/update-index db dec)
            (history.handlers/finalize now [::lower-selection
                                            "Lower selection"]))}))

(rf/reg-event-fx
 ::raise-to-top
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/update-index db (fn [_i sibling-count]
                                                (dec sibling-count)))
            (history.handlers/finalize now [::raise-selection-top
                                            "Raise selection to top"]))}))

(rf/reg-event-fx
 ::lower-to-bottom
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/update-index db #(identity 0))
            (history.handlers/finalize now [::lower-selection-bottom
                                            "Lower selection to bottom"]))}))

(rf/reg-event-fx
 ::align
 (fn [{:keys [db now]} [_ direction]]
   {:db (-> (element.handlers/align db direction)
            (history.handlers/finalize now
                                       [::update "Update %1"]
                                       [direction]))}))

(rf/reg-event-fx
 ::paste
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/paste db)
            (history.handlers/finalize now [::paste-selection
                                            "Paste selection"]))}))

(rf/reg-event-fx
 ::paste-in-place
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/paste-in-place db)
            (history.handlers/finalize now [::paste-selection-in-place
                                            "Paste selection in place"]))}))

(rf/reg-event-fx
 ::paste-styles
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/paste-styles db)
            (history.handlers/finalize now [::paste-styles-to-selection
                                            "Paste styles to selection"]))}))

(rf/reg-event-fx
 ::duplicate
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/duplicate db)
            (history.handlers/finalize now [::duplicate-selection
                                            "Duplicate selection"]))}))

(rf/reg-event-fx
 ::translate
 (fn [{:keys [db now]} [_ offset]]
   {:db (-> (element.handlers/translate db offset)
            (history.handlers/finalize now [::move-selection
                                            "Move selection"]))}))

(rf/reg-event-fx
 ::place
 (fn [{:keys [db now]} [_ position]]
   {:db (-> (element.handlers/place db position)
            (history.handlers/finalize now [::place-selection
                                            "Place selection"]))}))

(rf/reg-event-fx
 ::scale
 (fn [{:keys [db now]} [_ ratio]]
   (let [pivot-point (-> db element.handlers/bbox utils.bounds/center)]
     {:db (-> (element.handlers/scale db ratio pivot-point false)
              (history.handlers/finalize now [::scale-selection
                                              "Scale selection"]))})))

(rf/reg-event-fx
 ::->path
 (fn [{:keys [db]}]
   {::element.effects/->path
    {:data (element.handlers/selected db)
     :on-success [::finalize->path]
     :on-error [::app.events/toast-error]}}))

(rf/reg-event-fx
 ::finalize->path
 (fn [{:keys [db now]} [_ elements]]
   {:db (-> (reduce element.handlers/swap db elements)
            (history.handlers/finalize now [::convert-selection-path
                                            "Convert selection to path"]))}))

(rf/reg-event-fx
 ::stroke->path
 (fn [{:keys [db]}]
   {::element.effects/->path
    {:data (element.handlers/selected db)
     :on-success [::finalize-stroke->path]
     :on-error [::app.events/toast-error]}}))

(rf/reg-event-fx
 ::finalize-stroke->path
 (fn [{:keys [db now]} [_ elements]]
   {:db (-> (reduce element.handlers/swap db elements)
            (element.handlers/stroke->path)
            (history.handlers/finalize
             now
             [::convert-selection-stroke-path
              "Convert selection's stroke to path"]))}))

(rf/reg-event-fx
 ::boolean-operation
 (fn [{:keys [db]} [_ operation]]
   (when (seq (rest (element.handlers/selected db)))
     {::element.effects/->path
      {:data (element.handlers/selected db)
       :on-success [::finalize-boolean-operation operation]
       :on-error [::app.events/toast-error]}})))

(rf/reg-event-fx
 ::finalize-boolean-operation
 (fn [{:keys [db now]} [_ operation elements]]
   {:db (-> (reduce element.handlers/swap db elements)
            (element.handlers/boolean-operation operation)
            (history.handlers/finalize now
                                       (case operation
                                         :unite [::element.core/unite]
                                         :intersect [::element.core/intersect]
                                         :subtract [::element.core/subtract]
                                         :exclude [::element.core/exclude]
                                         :divide [::element.core/divide])))}))

(rf/reg-event-fx
 ::add
 (fn [{:keys [db now]} [_ el]]
   {:db (-> (element.handlers/add db el)
            (history.handlers/finalize now
                                       [::create "Create %1"]
                                       [(name (:tag el))]))}))

(rf/reg-event-fx
 ::import-svg
 (fn [{:keys [db now]} [_ data]]
   {:db (-> (element.handlers/import-svg db data)
            (history.handlers/finalize now [::import-svg "Import svg"]))}))

(rf/reg-event-fx
 ::animate
 (fn [{:keys [db now]} [_ tag attrs]]
   {:db (-> (element.handlers/animate db tag attrs)
            (history.handlers/finalize now
                                       (case tag
                                         :animate
                                         [::element.core/animate]

                                         :animateTransform
                                         [::element.core/animate-transform]

                                         :animateMotion
                                         [::element.core/animate-motion])))}))

(rf/reg-event-fx
 ::set-parent
 (fn [{:keys [db now]} [_ id parent-id]]
   {:db (-> (element.handlers/set-parent db id parent-id)
            (history.handlers/finalize now [::set-parent "Set parent"]))}))

(rf/reg-event-fx
 ::group
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/group db)
            (history.handlers/finalize now [::group-selection
                                            "Group selection"]))}))

(rf/reg-event-fx
 ::ungroup
 (fn [{:keys [db now]}]
   {:db (-> (element.handlers/ungroup db)
            (history.handlers/finalize now [::ungroup-selection
                                            "Ungroup selection"]))}))

(rf/reg-event-fx
 ::manipulate-path
 (fn [{:keys [db now]} [_ action]]
   {:db (-> (element.handlers/manipulate-path db action)
            (history.handlers/finalize now (case action
                                             :simplify
                                             [::element.core/path-simplify]

                                             :smooth
                                             [::element.core/path-smooth]

                                             :flatten
                                             [::element.core/path-flatten]

                                             :reverse
                                             [::element.core/path-reverse])))}))

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
 (fn [{:keys [db now]} _]
   (let [els (element.handlers/top-selected-sorted db)]
     {:db (-> (element.handlers/copy db)
              (element.handlers/delete)
              (history.handlers/finalize now [::cut-selection "Cut selection"]))
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

(rf/reg-event-fx
 ::create-traced-image
 (fn [{:keys [db now]} [_ data]]
   {:db (-> (element.handlers/import-svg db data)
            (history.handlers/finalize now [::trace-image "Trace image"]))}))

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
