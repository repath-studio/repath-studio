(ns renderer.panel.views
  (:require
   ["react-resizable-panels" :refer [Group Panel Separator useDefaultLayout]]
   [reagent.core :refer [defc]]))

(defn separator []
  [:> Separator
   {:class "relative after:block after:absolute after:z-1
            after:transition-colors after:duration-300 after:delay-300
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
            data-[separator=hover]:after:bg-accent!
            focus-visible:outline-hidden
            focus-visible:after:bg-border focus-visible:after:delay-0"}])

(defn panel
  [props & children]
  (into [:> Panel props] children))

(defc group
  [{:keys [id]
    :as props} & children]
  (let [layout (useDefaultLayout #js {:groupId id,
                                      :storage js/localStorage})]
    (into [:> Group
           (merge {:defaultLayout (.-defaultLayout layout)
                   :onLayoutChange (.-onLayoutChange layout)}
                  props)]
          children)))
