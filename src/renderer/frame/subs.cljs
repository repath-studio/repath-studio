(ns renderer.frame.subs
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.frame.handlers :as frame.handlers]))

(rf/reg-sub
 ::viewbox
 :<- [::document.subs/zoom]
 :<- [::document.subs/pan]
 :<- [::app.subs/dom-rect]
 (fn [[zoom pan dom-rect] _]
   (frame.handlers/viewbox zoom pan dom-rect)))

(rf/reg-sub
 ::viewbox-attr
 :<- [::viewbox]
 (fn [viewbox _]
   (string/join " " viewbox)))

(rf/reg-sub
 ::viewbox-bounds
 :<- [::viewbox]
 (fn [viewbox _]
   (let [[x y w h] viewbox
         [w h] (matrix/add [w h] [x y])]
     [x y w h])))
