(ns pages.components
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [re-frame.core :as rf]
   [renderer.i18n.subs]
   [renderer.icon.defaults :as icon.defaults]
   [renderer.views :as views]))

(rf/reg-sub
 :renderer.icon.subs/icon
 (fn [_ [_ id]]
   (get icon.defaults/icons id)))

(defscene ^:export buttons
  :title "Buttons"
  :params (atom false)
  [store]
  [views/toolbar
   {:class "bg-primary"}
   [views/icon-button
    "download"
    {:title "download"
     :on-click #(js/alert "Downloaded")}]
   [views/icon-button
    "folder"
    {:title "open"
     :on-click #(js/alert "Opened")}]
   [views/icon-button
    "save"
    {:title "save"
     :disabled true
     :on-click #(js/alert "Saved")}]
   [:span.v-divider]
   [views/radio-icon-button
    "refresh"
    @store
    {:title "Replay"
     :on-click #(swap! store not)}]])

(defscene ^:export switch
  :title "Switch"
  :params (atom true)
  [store]
  [views/toolbar
   {:class "bg-primary h-10 gap-2"}
   [views/switch
    "Default"
    {:id "default-switch"
     :default-checked @store
     :on-checked-change (fn [v] (reset! store v))}]
   [views/switch
    "Disabled"
    {:id "disabled-switch"
     :disabled true
     :default-checked @store
     :on-checked-change (fn [v] (reset! store v))}]
   [views/switch
    "Custom"
    {:id "custom-switch"
     :class "data-[state=checked]:bg-cyan-500"
     :default-checked @store
     :on-checked-change (fn [v] (reset! store v))}]
   [:span.v-divider]
   [:div (str "State: " @store)]])

(defscene ^:export slider
  :title "Slider"
  :params (atom [25])
  [store]
  [views/toolbar
   {:class "bg-primary flex gap-2 px-2"}
   [:div.w-64.h-8
    [views/slider
     {:min 0
      :max 50
      :step 1
      :default-value @store
      :on-value-change (fn [v] (reset! store v))}]]
   [:div.w-64.h-8
    [views/slider
     {:min 0
      :max 50
      :step 1
      :disabled true
      :default-value @store
      :on-value-change (fn [v] (reset! store v))}]]
   [:span.v-divider]
   [:div (first @store)]])

(defscene ^:export default
  :title "Icons"
  []
  [:div.flex
   [:div.flex.flex-wrap.gap-2.p-3
    (for [[k _v] icon.defaults/icons]
      ^{:key k}
      [:div {:title k}
       [views/icon k]])]
   [:div.flex.p-3
    [views/icon "download"
     {:class "text-accent"}]]])
