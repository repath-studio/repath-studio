(ns renderer.utils.compatibility
  (:require
   [malli.core :as m]
   [renderer.utils.migration :as utils.migration]))

(def ver-regex
  "https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string"
  #"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)
    (?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)
    (?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?
    (?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$")

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
  [document-version migration-version]
  (let [[m-major m-minor m-patch] migration-version
        [d-major d-minor d-patch] document-version]
    (or (< d-major m-major)
        (and (= d-major m-major)
             (< d-minor m-minor))
        (and (= d-major m-major)
             (= d-minor m-minor)
             (< d-patch m-patch)))))

(m/=> migrate-document [:function
                        [:-> map? map?]
                        [:-> map? [:tuple SemanticVersion ifn?] map?]])
(defn migrate-document
  ([document]
   (reduce migrate-document document utils.migration/migrations))
  ([document [ver f]]
   (cond-> document
     (:or (not (:version document))
          (requires-migration? (version->vec (:version document)) ver))
     f)))
