(ns renderer.frame.subs
  (:require
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
 :-> (partial apply frame.handlers/viewbox))

(rf/reg-sub
 ::viewbox-attr
 :<- [::viewbox]
 :-> (partial string/join " "))

(rf/reg-sub
 ::viewbox-bounds
 :<- [::viewbox]
 :-> frame.handlers/viewbox->bounds)
