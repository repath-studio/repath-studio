(ns config)

(goog-define ^js/String version "unknown")
(goog-define ^js/String SENTRY-DSN "unknown")

(def debug? ^boolean goog.DEBUG)

(def ext "rps")

(def app-name "Repath Studio")

(def mime-type "application/x-repath-studio")

(def default-path "documents")

(def save-info-keys
  "These are the keys that are saved in the recent documents list.
   The opposite is true for saved documents, that shouldn't maintain keys
   generated on document load."
  [:id :title :path])

(def sentry {:dsn SENTRY-DSN
             :environment (if debug? "development" "production")
             :release version
             :debug debug?})
