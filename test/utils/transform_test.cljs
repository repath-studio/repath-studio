(ns utils.transform-test
  (:require
   [cljs.test :refer-macros [deftest testing is]]
   [clojure.test.check :as tc]
   [clojure.test.check.properties :as prop]
   [generators :as generators]
   [renderer.utils.transform :as utils.transform :refer [identity-transform]]))

(defn vec-near? [a b]
  (every? (fn [[x y]]
            (< (Math/abs (- x y)) 1e-10))
          (map vector a b)))

(deftest test-point-identity
  (testing "point transform with identity returns the point"
    (is (tc/quick-check 100
                        (prop/for-all [p generators/point]
                                      (let [r (-> identity-transform
                                                  (utils.transform/point p))]
                                        (= p r)))))))

(deftest test-compose-identity
  (testing "composing with identity returns the original matrix"
    (is (tc/quick-check 100
                        (prop/for-all [m generators/matrix]
                                      (let [id identity-transform
                                            r1 (utils.transform/compose m id)
                                            r2 (utils.transform/compose id m)]
                                        (and (vec-near? r1 m)
                                             (vec-near? r2 m))))))))

(deftest test-compose-inverse
  (testing "composing a matrix with its inverse gives identity"
    (is (tc/quick-check 100
                        (prop/for-all [m generators/matrix]
                                      (let [r (-> (utils.transform/inverse m)
                                                  (utils.transform/compose m))]
                                        (vec-near? r identity-transform)))))))

(deftest test-point-compositional
  (testing "point(compose m1 m2, p) = point(m1, point(m2, p))"
    (is (tc/quick-check
         100
         (prop/for-all [m1 generators/matrix
                        m2 generators/matrix
                        p generators/point]
                       (let [r1 (-> (utils.transform/compose m1 m2)
                                    (utils.transform/point p))
                             r2 (->> (utils.transform/point m2 p)
                                     (utils.transform/point m1))]
                         (vec-near? r1 r2)))))))

(deftest test-inverse-point
  (testing "inverse-point(m, p) maps back to p when transformed by m"
    (is (tc/quick-check
         100
         (prop/for-all [m generators/matrix p generators/point]
                       (let [r (->> (utils.transform/inverse-point m p)
                                    (utils.transform/point m))]
                         (vec-near? r p)))))))

(deftest test-aabb-identity
  (testing "aabb of identity transform returns the original bbox"
    (is (tc/quick-check
         100
         (prop/for-all [x1 generators/coord x2 generators/coord
                        y1 generators/coord y2 generators/coord]
                       (let [w (max x1 x2)
                             h (max y1 y2)
                             bbox [x1 y1 w h]]
                         (= (utils.transform/aabb identity-transform bbox)
                            bbox)))))))

(deftest test-translate-inverse
  (testing "translate composed with its inverse gives identity"
    (is (tc/quick-check
         100
         (prop/for-all [tx generators/coord ty generators/coord]
                       (let [t1 (utils.transform/translate tx ty)
                             t2 (utils.transform/translate (- tx) (- ty))]
                         (vec-near? (utils.transform/compose t1 t2)
                                    identity-transform)))))))

(deftest test-scale-inverse
  (testing "scale composed with its inverse gives identity"
    (is (tc/quick-check
         100
         (prop/for-all [sx generators/nz-coord sy generators/nz-coord]
                       (let [s1 (utils.transform/scale [sx sy])
                             s2 (utils.transform/scale [(/ 1 sx) (/ 1 sy)])]
                         (vec-near? (utils.transform/compose s1 s2)
                                    identity-transform)))))))
