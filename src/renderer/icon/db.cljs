(ns renderer.icon.db
  (:require
   [malli.core :as m]))

(def IconId string?)

(def IconPath
  "A string of SVG path data.
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/d"
  [:and
   :string
   [:re #"^[MmZzLlHhVvCcSsQqTtAa0-9\-,.\s]*$"]
   [:fn {:error/message "must start with a path command"}
    #(not (re-find #"^[0-9\-,.]" %))]
   [:fn {:error/message "must not end with an operator"}
    #(not (re-find #"[\-,.]\s*$" %))]])

(def Icon
  [:map {:closed true}
   [:id IconId]
   [:path IconPath]])

(def Icons
  [:map-of IconId Icon])

(def valid-icon? (m/validator Icon))

(def explain-icon (m/explainer Icon))
