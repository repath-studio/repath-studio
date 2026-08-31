(ns renderer.window.handlers
  (:require
   [malli.core :as m]
   [renderer.window.db :refer [Breakpoint]]))

(m/=> breakpoint? [:-> number? Breakpoint])
(defn breakpoint?
  [width breakpoint]
  ;; https://tailwindcss.com/docs/responsive-design#overview
  (>= width (get {:2xl 1536
                  :xl 1280
                  :lg 1024
                  :md 768
                  :sm 640} breakpoint)))
