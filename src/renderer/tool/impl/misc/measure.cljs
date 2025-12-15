(ns renderer.tool.impl.misc.measure
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.handlers :as app.handlers]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.handlers :as element.handlers]
   [renderer.i18n.views :as i18n.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.length :as utils.length]
   [renderer.utils.math :as utils.math]
   [renderer.utils.svg :as utils.svg]))

(tool.hierarchy/derive-tool :measure ::tool.hierarchy/tool)

(defonce measure-attrs (reagent/atom nil))

(rf/reg-fx
 ::set-measure-attrs
 (fn [value]
   (reset! measure-attrs value)))

(defmethod tool.hierarchy/properties :measure
  []
  {:icon "ruler-triangle"
   :label [::label "Measure"]})

(defmethod tool.hierarchy/help [:measure :idle]
  []
  (i18n.views/t [::help "Click and drag to measure a distance."]))

(defmethod tool.hierarchy/on-activate :measure
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(defmethod tool.hierarchy/on-deactivate :measure
  [db]
  (app.handlers/add-fx db [::set-measure-attrs nil]))

(defmethod tool.hierarchy/on-drag :measure
  [db _e]
  (let [[offset-x offset-y] (tool.handlers/snapped-offset db)
        [x y] (tool.handlers/snapped-position db)
        [adjacent opposite] (matrix/sub [offset-x offset-y] [x y])
        hypotenuse (Math/hypot adjacent opposite)]
    (app.handlers/add-fx db [::set-measure-attrs {:x1 offset-x
                                                  :y1 offset-y
                                                  :x2 x
                                                  :y2 y
                                                  :hypotenuse hypotenuse}])))

(defmethod tool.hierarchy/render :measure
  []
  (when @measure-attrs
    (let [{:keys [x1 x2 y1 y2 hypotenuse]} @measure-attrs
          [x1 y1 x2 y2] (map utils.length/unit->px [x1 y1 x2 y2])
          angle (utils.math/angle [x1 y1] [x2 y2])
          zoom @(rf/subscribe [::document.subs/zoom])
          straight? (< angle 180)
          straight-angle (if straight? angle (- angle 360))
          line-bg-attrs {:stroke "var(--accent-foreground)"
                         :stroke-opacity ".5"
                         :stroke-width 3}]
      [:g
       [utils.svg/arc [x1 y1] 20 (if straight? 0 angle) (abs straight-angle)]

       [utils.svg/line [x1 y1] [x2 y2] line-bg-attrs]
       [utils.svg/line [x1 y1] [(+ x1 (/ 30 zoom)) y1] line-bg-attrs]

       [utils.svg/line [x1 y1] [x2 y2]]
       [utils.svg/line [x1 y1] [(+ x1 (/ 30 zoom)) y1]]

       [utils.svg/cross [x1 y1]]
       [utils.svg/cross [x2 y2]]

       [utils.svg/label
        (str (utils.length/->fixed straight-angle 2 false) "°")
        {:x (+ x1 (/ 40 zoom))
         :y y1
         :text-anchor "start"}]

       [utils.svg/label
        (-> hypotenuse js/parseFloat (utils.length/->fixed 2 false))
        {:x (/ (+ x1 x2) 2)
         :y (/ (+ y1 y2) 2)}]])))

(defmethod tool.hierarchy/snapping-points :measure
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label (if @measure-attrs
               [::measure-end "measure end"]
               [::measure-start "measure start"])})])

(defmethod tool.hierarchy/snapping-elements :measure
  [db]
  (element.handlers/visible db))
