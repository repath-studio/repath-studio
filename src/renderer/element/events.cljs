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
   [renderer.history.events :refer [finalize]]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.extra :refer [partial-right]]))

(rf/reg-event-db
 ::select
 [(finalize (fn [[_ _id additive]]
              (if additive
                [[::modify-selection "Modify selection"]]
                [[::select-element "Select element"]])))]
 (fn [db [_ id additive]]
   (element.handlers/toggle-selection db id additive)))

(rf/reg-event-db
 ::select-ids
 [(finalize [::select-elements "Select elements"])]
 (fn [db [_ ids]]
   (-> (partial-right element.handlers/assoc-prop :selected true)
       (reduce (element.handlers/deselect db) ids))))

(rf/reg-event-db
 ::toggle-prop
 [(finalize (fn [[_ _id _k explanation]] [explanation]))]
 (fn [db [_ id k _explanation]]
   (element.handlers/update-prop db id k not)))

(rf/reg-event-db
 ::set-label
 [(finalize [::set-label "Set label"])]
 (fn [db [_ id v]]
   (element.handlers/assoc-prop db id :label v)))

(rf/reg-event-db
 ::lock
 [(finalize [::lock-selection "Lock selection"])]
 (fn [db _]
   (element.handlers/assoc-prop db :locked true)))

(rf/reg-event-db
 ::unlock
 [(finalize [::unlock-selection "Unlock selection"])]
 (fn [db _]
   (element.handlers/assoc-prop db :locked false)))

(rf/reg-event-db
 ::set-attr
 [(finalize (fn [[_ k]] [[::set "Set %1"] [(name k)]]))]
 (fn [db [_ k v]]
   (element.handlers/set-attr db k v)))

(rf/reg-event-db
 ::remove-attr
 [(finalize
   (fn [[_ k]] [[::remove "Remove %1"] [(name k)]]))]
 (fn [db [_ k]]
   (element.handlers/dissoc-attr db k)))

(rf/reg-event-db
 ::update-attr
 [(finalize (fn [[_ k]] [[::update "Update %1"] [(name k)]]))]
 (fn [db [_ k f & more]]
   (-> (apply partial-right element.handlers/update-attr k f more)
       (reduce db (element.handlers/selected-ids db)))))

(rf/reg-event-db
 ::preview-attr
 (fn [db [_ k v]]
   (cond-> db
     (= (:state db) :idle)
     (element.handlers/set-attr db k v))))

(rf/reg-event-db
 ::delete
 [(finalize [::delete-selection "Delete selection"])]
 (fn [db _]
   (element.handlers/delete db)))

(rf/reg-event-db
 ::deselect-all
 [(finalize [::deselect-all "Deselect all"])]
 (fn [db _]
   (element.handlers/deselect db)))

(rf/reg-event-db
 ::select-all
 [(finalize [::select-all "Select all"])]
 (fn [db _]
   (element.handlers/select-all db)))

(rf/reg-event-db
 ::select-same-tags
 [(finalize [::select-same-tags "Select same tags"])]
 (fn [db _]
   (element.handlers/select-same-tags db)))

(rf/reg-event-db
 ::invert-selection
 [(finalize [::invert-selection "Invert selection"])]
 (fn [db _]
   (element.handlers/invert-selection db)))

(rf/reg-event-db
 ::raise
 [(finalize [::raise-selection "Raise selection"])]
 (fn [db _]
   (element.handlers/update-index db inc)))

(rf/reg-event-db
 ::lower
 [(finalize [::lower-selection "Lower selection"])]
 (fn [db _]
   (element.handlers/update-index db dec)))

(rf/reg-event-db
 ::raise-to-top
 [(finalize [::raise-selection-top
             "Raise selection to top"])]
 (fn [db _]
   (element.handlers/update-index db (fn [_i sibling-count]
                                       (dec sibling-count)))))

(rf/reg-event-db
 ::lower-to-bottom
 [(finalize [::lower-selection-bottom "Lower selection to bottom"])]
 (fn [db _]
   (element.handlers/update-index db #(identity 0))))

(rf/reg-event-db
 ::align
 [(finalize (fn [[_ direction]] [[::update "Update %1"] [direction]]))]
 (fn [db [_ direction]]
   (element.handlers/align db direction)))

(rf/reg-event-db
 ::paste
 [(finalize [::paste-selection "Paste selection"])]
 (fn [db _]
   (element.handlers/paste db)))

(rf/reg-event-db
 ::paste-in-place
 [(finalize [::paste-selection-in-place
             "Paste selection in place"])]
 (fn [db _]
   (element.handlers/paste-in-place db)))

(rf/reg-event-db
 ::paste-styles
 [(finalize [::paste-styles-to-selection "Paste styles to selection"])]
 (fn [db _]
   (element.handlers/paste-styles db)))

(rf/reg-event-db
 ::duplicate
 [(finalize [::duplicate-selection
             "Duplicate selection"])]
 (fn [db _]
   (element.handlers/duplicate db)))

(rf/reg-event-db
 ::translate
 [(finalize [::move-selection "Move selection"])]
 (fn [db [_ offset]]
   (element.handlers/translate db offset)))

(rf/reg-event-db
 ::place
 [(finalize [::place-selection "Place selection"])]
 (fn [db [_ position]]
   (element.handlers/place db position)))

(rf/reg-event-db
 ::scale
 [(finalize [::scale-selection "Scale selection"])]
 (fn [db [_ ratio]]
   (let [pivot-point (-> db element.handlers/bbox utils.bounds/center)]
     (element.handlers/scale db ratio pivot-point false))))

(rf/reg-event-fx
 ::->path
 (fn [{:keys [db]}]
   (when (= (:state db) :idle)
     {:db (tool.handlers/activate db :transform)
      ::element.effects/->path
      {:data (element.handlers/selected db)
       :on-success [::finalize->path]
       :on-error [::app.events/toast-error]}})))

(rf/reg-event-db
 ::finalize->path
 [(finalize [::convert-selection-path "Convert selection to path"])]
 (fn [db [_ elements]]
   (reduce element.handlers/swap db elements)))

(rf/reg-event-fx
 ::stroke->path
 (fn [{:keys [db]}]
   (when (= (:state db) :idle)
     {:db (tool.handlers/activate db :transform)
      ::element.effects/->path
      {:data (element.handlers/selected db)
       :on-success [::finalize-stroke->path]
       :on-error [::app.events/toast-error]}})))

(rf/reg-event-db
 ::finalize-stroke->path
 [(finalize [::convert-selection-stroke-path
             "Convert selection's stroke to path"])]
 (fn [db [_ elements]]
   (-> (reduce element.handlers/swap db elements)
       (element.handlers/stroke->path))))

(rf/reg-event-fx
 ::boolean-operation
 (fn [{:keys [db]} [_ operation]]
   (when (and (= (:state db) :idle)
              (seq (rest (element.handlers/selected db))))
     {:db (tool.handlers/activate db :transform)
      ::element.effects/->path
      {:data (element.handlers/selected db)
       :on-success [::finalize-boolean-operation operation]
       :on-error [::app.events/toast-error]}})))

(rf/reg-event-db
 ::finalize-boolean-operation
 [(finalize (fn [[_ operation]]
              [(case operation
                 :unite [::element.core/unite]
                 :intersect [::element.core/intersect]
                 :subtract [::element.core/subtract]
                 :exclude [::element.core/exclude]
                 :divide [::element.core/divide])]))]
 (fn [db [_ operation elements]]
   (-> (reduce element.handlers/swap db elements)
       (element.handlers/boolean-operation operation))))

(rf/reg-event-db
 ::add
 [(finalize (fn [[_ el]] [[::create "Create %1"] [(name (:tag el))]]))]
 (fn [db [_ el]]
   (element.handlers/add db el)))

(rf/reg-event-db
 ::import-svg
 [(finalize [::import-svg "Import svg"])]
 (fn [db [_ data]]
   (element.handlers/import-svg db data)))

(rf/reg-event-db
 ::animate
 [(finalize (fn [[_ tag]]
              [(case tag
                 :animate [::element.core/animate]
                 :animateTransform [::element.core/animate-transform]
                 :animateMotion [::element.core/animate-motion])]))]
 (fn [db [_ tag attrs]]
   (element.handlers/animate db tag attrs)))

(rf/reg-event-db
 ::set-parent
 [(finalize [::set-parent "Set parent"])]
 (fn [db [_ id parent-id]]
   (element.handlers/set-parent db id parent-id)))

(rf/reg-event-db
 ::group
 [(finalize [::group-selection "Group selection"])]
 (fn [db _]
   (element.handlers/group db)))

(rf/reg-event-db
 ::ungroup
 [(finalize [::ungroup-selection "Ungroup selection"])]
 (fn [db _]
   (element.handlers/ungroup db)))

(rf/reg-event-db
 ::manipulate-path
 [(finalize (fn [[_ action]]
              [(case action
                 :simplify [::element.core/path-simplify]
                 :smooth [::element.core/path-smooth]
                 :flatten [::element.core/path-flatten]
                 :reverse [::element.core/path-reverse])]))]
 (fn [db [_ action]]
   (element.handlers/manipulate-path db action)))

(rf/reg-event-fx
 ::copy
 (fn [{:keys [db]} _]
   (let [els (element.handlers/top-selected-sorted db)]
     (when (= (:state db) :idle)
       {:db (-> (tool.handlers/activate db :transform)
                (element.handlers/copy))
        :fx [(when (seq els)
               [::effects/clipboard-write
                {:data (utils.element/->svg els)
                 :on-error [::app.events/toast-error]}])]}))))

(rf/reg-event-fx
 ::cut
 [(finalize [::cut-selection "Cut selection"])]
 (fn [{:keys [db]} _]
   (let [els (element.handlers/top-selected-sorted db)]
     {:db (-> (element.handlers/copy db)
              (element.handlers/delete))
      :fx [(when (seq els)
             [::effects/clipboard-write
              {:data (utils.element/->svg els)
               :on-error [::app.events/toast-error]}])]})))

(rf/reg-event-fx
 ::trace
 (fn [{:keys [db]} [_]]
   (let [images (element.handlers/filter-selected-by-tag db :image)]
     (when (= (:state db) :idle)
       {:db (tool.handlers/activate db :transform)
        ::element.effects/trace {:data images
                                 :on-success [::create-traced-image]}}))))

(rf/reg-event-db
 ::create-traced-image
 [(finalize [::trace-image "Trace image"])]
 (fn [db [_ data]]
   (element.handlers/import-svg db data)))

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
