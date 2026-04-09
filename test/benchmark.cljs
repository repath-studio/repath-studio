(ns benchmark
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [clojure.string :as string]
   [day8.re-frame.test :as rf.test]
   [malli.instrument :as m.instrument]
   [re-frame.core :as rf]
   [renderer.app.events :as app.events]
   [renderer.element.events :as-alias element.events]))

(defn bench
  "Returns the elapsed time of the event handling in milliseconds."
  ([e]
   (bench e 1))
  ([e iterations]
   (let [start (.getTime (js/Date.))
         _ (dotimes [_ iterations] (rf/dispatch e))
         end (.getTime (js/Date.))]
     (- end start))))

(deftest polygons
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   ;; Instrumentation and db validation affects performance, so we disable it.
   (m.instrument/unstrument!)
   (rf/clear-global-interceptor ::app.events/schema-validator)

   (testing "creating elements"
     (let [points (string/join " " (repeatedly 100 #(rand-int 1000)))]
       (is (> 1000 (bench [::element.events/add {:tag :polygon
                                                 :attrs {:points points}}]
                          20)))))

   (testing "selecting elements"
     (is (> 1000 (bench [::element.events/select-all]))))

   (testing "deselecting elements"
     (is (> 1000 (bench [::element.events/deselect-all]))))

   (testing "moving elements"
     (rf/dispatch [::element.events/select-all])
     (is (> 100 (bench [::element.events/translate [100 100]]))))

   (testing "scaling elements"
     (is (> 100 (bench [::element.events/scale [100 100]]))))

   (m.instrument/instrument!)
   (rf/reg-global-interceptor app.events/schema-validator)))
