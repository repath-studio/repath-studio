(ns generators
  (:require [clojure.test.check.generators :as gen]
            [renderer.utils.transform :as utils.transform]))

(def coord (gen/fmap double (gen/choose -100 100)))

(def nz-coord (gen/such-that (complement zero?) coord))

(def point (gen/tuple coord coord))

(def bbox
  (gen/fmap (fn [x1 x2 y1 y2]
              [x1 y1 (max x1 x2) (max y1 y2)])
            (gen/tuple coord coord coord coord)))

(def matrix
  (gen/fmap (fn [[tx ty sx sy deg]]
              (-> (utils.transform/translate tx ty)
                  (utils.transform/compose (utils.transform/scale [sx sy]))
                  (utils.transform/compose (utils.transform/rotate deg))))
            (gen/tuple coord coord nz-coord nz-coord coord)))
