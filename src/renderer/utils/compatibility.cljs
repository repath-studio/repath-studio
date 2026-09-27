(ns renderer.utils.compatibility
  (:require
   [malli.core :as m]))

(def ver-regex
  "https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string"
  (re-pattern (str "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
                   "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)"
                   "(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?"
                   "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$")))

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

(m/=> -requires-migration? [:-> SemanticVersion SemanticVersion boolean?])
(defn -requires-migration?
  [from-version to-version]
  (let [[m-major m-minor m-patch] to-version
        [d-major d-minor d-patch] from-version]
    (or (< d-major m-major)
        (and (= d-major m-major)
             (< d-minor m-minor))
        (and (= d-major m-major)
             (= d-minor m-minor)
             (< d-patch m-patch)))))

(m/=> requires-migration? [:-> map? SemanticVersion boolean?])
(defn requires-migration?
  "Checks if the provided map requires migration to the given version.

   Returns true if the map's version is older than the given version,
   or if the map has no version (e.g. a document created before versioning was
   introduced to the document schema)."
  [m version]
  (or (not (:version m))
      (and (not= (:version m) "unknown")
           (-> (version->vec (:version m))
               (-requires-migration? version)))))

(m/=> migrate [:-> map? [:vector [:tuple SemanticVersion ifn?]] map?])
(defn migrate
  [m migrations]
  (reduce (fn [m [version f]]
            (cond-> m
              (and m (requires-migration? m version))
              (f))) m migrations))
