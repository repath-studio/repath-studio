(ns config)

(goog-define ^js/String version "unknown")
(goog-define ^js/String SENTRY-DSN "unknown")

(def debug? ^boolean goog.DEBUG)

(def ext "rps")

(def app-name "Repath Studio")

(def mime-type "application/x-repath-studio")

(def default-path "documents")

(def max-recent-documents 10)

(def min-zoom 0.01)

(def max-zoom 100)

(def max-shell-history 200)

(def max-shell-completions 50)

(def image-mime-types
  {"image/png" [".png"]
   "image/jpeg" [".jpeg" ".jpg"]
   "image/bmp" [".bmp"]
   "image/gif" [".gif"]
   "image/webp" [".webp"]})

(def save-info-keys
  "These are the keys that are saved in the recent documents list.
   The opposite is true for saved documents, that shouldn't maintain keys
   generated on document load."
  [:id :title :path])

(def sentry {:dsn SENTRY-DSN
             :environment (if debug? "development" "production")
             :release version
             :debug debug?})
