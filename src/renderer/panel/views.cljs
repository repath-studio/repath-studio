(ns renderer.panel.views
  (:require
   ["react-resizable-panels" :refer [Group Panel Separator useDefaultLayout]]
   [goog.functions]
   [re-frame.core :as rf]
   [reagent.core :refer [defc]]
   [renderer.i18n.views :as i18n.views]
   [renderer.panel.events :as-alias panel.events]
   [renderer.views :as views]))

(defn separator []
  [:> Separator
   {:class "relative after:block after:absolute after:z-1
            after:transition-colors after:duration-300 after:delay-100
            focus:after:delay-0
            aria-[orientation=horizontal]:h-px
            aria-[orientation=horizontal]:after:w-full
            aria-[orientation=horizontal]:after:h-1
            aria-[orientation=horizontal]:after:top-[-50%]
            aria-[orientation=vertical]:w-px
            aria-[orientation=vertical]:after:h-full
            aria-[orientation=vertical]:after:w-1
            aria-[orientation=vertical]:after:left-[-50%]
            data-[separator=active]:after:bg-accent!
            focus-visible:outline-hidden
            focus-visible:after:bg-border focus-visible:after:delay-0"}])

(defn close-button
  [id]
  [views/icon-button "window-close"
   {:title (i18n.views/t [::close-panel "Close panel"])
    :class "panel-close-button absolute z-1 top-1 right-1 rtl:right-auto
            rtl:left-1 bg-transparent! invisible"
    :on-click #(rf/dispatch [::panel.events/toggle id])}])

(defc panel
  [props & children]
  (into [:> Panel (views/merge-with-class
                   {:class "hover:[&_>.panel-close-button]:visible!"}
                   props)]
        children))

(defc group
  [{:keys [id]
    :as props} & children]
  (let [layout (useDefaultLayout #js {:id id,
                                      :storage js/localStorage})]
    (js/console.log (.-defaultLayout layout))
    (into [:> Group
           (merge {:defaultLayout (.-defaultLayout layout)
                   :onLayoutChange (.-onLayoutChange layout)}
                  props)]
          children)))
