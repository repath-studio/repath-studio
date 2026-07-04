(ns renderer.utils.compatibility
  (:require
   [malli.core :as m]))

(def ver-regex
  "https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string"
  #"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$")

(def SemanticVersion
  [:tuple
   [number? {:title "major"}]
   [number? {:title "minor"}]
   [number? {:title "patch"}]])

(m/=> version->vec [:-> string? SemanticVersion])
(defn version->vec
  [s]
  (into []
        (comp (drop 1)
              (take 3)
              (map js/parseInt))
        (re-find ver-regex s)))

(m/=> requires-migration? [:-> SemanticVersion SemanticVersion boolean?])
(defn requires-migration?
  [from-version to-version]
  (let [[m-major m-minor m-patch] to-version
        [d-major d-minor d-patch] from-version]
    (or (< d-major m-major)
        (and (= d-major m-major)
             (< d-minor m-minor))
        (and (= d-major m-major)
             (= d-minor m-minor)
             (< d-patch m-patch)))))
