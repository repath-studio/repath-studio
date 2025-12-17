(ns renderer.utils.key
  (:require
   [clojure.set :as set]
   [malli.core :as m]
   [renderer.app.events :as-alias app.events]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.document.events :as-alias document.events]
   [renderer.element.events :as-alias element.events]
   [renderer.frame.events :as-alias frame.events]
   [renderer.history.events :as-alias history.events]
   [renderer.menubar.events :as-alias menubar.events]
   [renderer.panel.events :as-alias panel.events]
   [renderer.ruler.events :as-alias ruler.events]
   [renderer.tool.events :as-alias tool.events]
   [renderer.window.events :as-alias window.events])
  (:import
   [goog.events KeyCodes]))

(defn arrow?
  [k]
  (contains? #{"ArrowUp" "ArrowDown" "ArrowLeft" "ArrowRight"} k))

(defn down-handler!
  "Generic on-key-down handler for uncontrolled input elements that dispatches
   an event `f` in order to update a db value on keyboard enter, or reset to the
   initial value `v` on escape.

   We need uncontrolled inputs to avoid updating the canvas with incomplete
   values while the user is typing, and also avoid polluting the history stack.

   The `default-value` attribute should be used to update the value reactively."
  [e v f & more]
  (let [target (.-target e)]
    (.stopPropagation e)

    (case (.-key e)
      "Enter" (do (apply f e more)
                  (.blur target))
      "Escape" (do (set! (.-value target) v)
                   (.blur target))
      nil)))

(def codes
  "https://google.github.io/closure-library/api/goog.events.KeyCodes.html"
  (js->clj KeyCodes))

(def key-chars
  (set/map-invert codes))

(m/=> code->key [:-> number? [:maybe string?]])
(defn code->key
  [key-code]
  (get key-chars key-code))

(def keydown-rules
  {:event-keys [[[::element.events/raise]
                 [{:keyCode (codes "PAGE_UP")}]]
                [[::element.events/lower]
                 [{:keyCode (codes "PAGE_DOWN")}]]
                [[::element.events/raise-to-top]
                 [{:keyCode (codes "HOME")}]]
                [[::element.events/lower-to-bottom]
                 [{:keyCode (codes "END")}]]
                [[::frame.events/focus-selection :original]
                 [{:keyCode (codes "ONE")}]]
                [[::frame.events/focus-selection :fit]
                 [{:keyCode (codes "TWO")}]]
                [[::frame.events/focus-selection :fill]
                 [{:keyCode (codes "THREE")}]]
                [[::frame.events/zoom-in]
                 [{:keyCode (codes "EQUALS")}]]
                [[::frame.events/zoom-out]
                 [{:keyCode (codes "DASH")}]]
                [[::element.events/->path]
                 [{:keyCode (codes "P")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.events/stroke->path]
                 [{:keyCode (codes "P")
                   :ctrlKey true
                   :altKey true}]]
                [[::panel.events/toggle :tree]
                 [{:keyCode (codes "T")
                   :ctrlKey true}]]
                [[::panel.events/toggle :properties]
                 [{:keyCode (codes "P")
                   :ctrlKey true}]]
                [[::panel.events/toggle :history]
                 [{:keyCode (codes "H")
                   :ctrlKey true}]]
                [[::app.events/toggle-grid]
                 [{:keyCode (codes "PERIOD")
                   :ctrlKey true}]]
                [[::ruler.events/toggle-visible]
                 [{:keyCode (codes "R")
                   :ctrlKey true}]]
                [[::element.events/copy]
                 [{:keyCode (codes "C")
                   :ctrlKey true}]]
                [[::element.events/paste-styles]
                 [{:keyCode (codes "V")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.events/paste-in-place]
                 [{:keyCode (codes "V")
                   :ctrlKey true
                   :altKey true}]]
                [[::element.events/paste]
                 [{:keyCode (codes "V")
                   :ctrlKey true}]]
                [[::element.events/cut]
                 [{:keyCode (codes "X")
                   :ctrlKey true}]]
                [[::app.events/toggle-debug-info]
                 [{:keyCode (codes "D")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.events/duplicate]
                 [{:keyCode (codes "D")
                   :ctrlKey true}]]
                [[::element.events/boolean-operation :exclude]
                 [{:keyCode (codes "E")
                   :ctrlKey true}]]
                [[::element.events/boolean-operation :unite]
                 [{:keyCode (codes "U")
                   :ctrlKey true}]]
                [[::element.events/boolean-operation :intersect]
                 [{:keyCode (codes "I")
                   :ctrlKey true}]]
                [[::element.events/boolean-operation :subtract]
                 [{:keyCode (codes "BACKSLASH")
                   :ctrlKey true}]]
                [[::element.events/boolean-operation :divide]
                 [{:keyCode (codes "SLASH")
                   :ctrlKey true}]]
                [[::element.events/ungroup]
                 [{:keyCode (codes "G")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.events/group]
                 [{:keyCode (codes "G")
                   :ctrlKey true}]]
                [[::element.events/unlock]
                 [{:keyCode (codes "L")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.events/lock]
                 [{:keyCode (codes "L")
                   :ctrlKey true}]]
                [[::element.events/delete]
                 [{:keyCode (codes "DELETE")}]
                 [{:keyCode (codes "BACKSPACE")}]]
                [[::document.events/new]
                 [{:keyCode (codes "N")
                   :ctrlKey true}]]
                [[::tool.events/cancel]
                 [{:keyCode (codes "ESC")}]]
                [[::history.events/redo]
                 [{:keyCode (codes "Z")
                   :ctrlKey true
                   :shiftKey true}]
                 [{:keyCode (codes "Y")
                   :ctrlKey true}]]
                [[::history.events/undo]
                 [{:keyCode (codes "Z")
                   :ctrlKey true}]]
                [[::element.events/select-same-tags]
                 [{:keyCode (codes "A")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.events/select-all]
                 [{:keyCode (codes "A")
                   :ctrlKey true}]]
                [[::menubar.events/activate :file]
                 [{:keyCode (codes "F")
                   :altKey true}]]
                [[::menubar.events/activate :edit]
                 [{:keyCode (codes "E")
                   :altKey true}]]
                [[::menubar.events/activate :object]
                 [{:keyCode (codes "O")
                   :altKey true}]]
                [[::menubar.events/activate :view]
                 [{:keyCode (codes "V")
                   :altKey true}]]
                [[::menubar.events/activate :help]
                 [{:keyCode (codes "H")
                   :altKey true}]]
                [[::window.events/close]
                 [{:keyCode (codes "Q")
                   :ctrlKey true}]]
                [[::document.events/open]
                 [{:keyCode (codes "O")
                   :ctrlKey true}]]
                [[::document.events/save-as]
                 [{:keyCode (codes "S")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::document.events/save]
                 [{:keyCode (codes "S")
                   :ctrlKey true}]]
                [[::document.events/close-active]
                 [{:keyCode (codes "W")
                   :ctrlKey true}]]
                [[::document.events/close-all]
                 [{:keyCode (codes "W")
                   :ctrlKey true
                   :altKey true}]]
                [[::window.events/toggle-fullscreen]
                 [{:keyCode (codes "F11")}]]
                [[::dialog.events/show-cmdk]
                 [{:keyCode (codes "F1")}]
                 [{:keyCode (codes "K")
                   :ctrlKey true}]]
                [[::tool.events/activate :edit]
                 [{:keyCode (codes "E")}]]
                [[::tool.events/activate :circle]
                 [{:keyCode (codes "C")}]]
                [[::tool.events/activate :line]
                 [{:keyCode (codes "L")}]]
                [[::tool.events/activate :text]
                 [{:keyCode (codes "T")}]]
                [[::tool.events/activate :pan]
                 [{:keyCode (codes "P")}]]
                [[::tool.events/activate :zoom]
                 [{:keyCode (codes "Z")}]]
                [[::tool.events/activate :rect]
                 [{:keyCode (codes "R")}]]
                [[::tool.events/activate :transform]
                 [{:keyCode (codes "S")}]]
                [[::tool.events/activate :fill]
                 [{:keyCode (codes "F")}]]]

   :clear-keys []

   :always-listen-keys []

   :prevent-default-keys [{:keyCode (codes "EQUALS")}
                          {:keyCode (codes "DASH")}
                          {:keyCode (codes "RIGHT")}
                          {:keyCode (codes "LEFT")}
                          {:keyCode (codes "UP")}
                          {:keyCode (codes "DOWN")}
                          {:keyCode (codes "F1")}
                          {:keyCode (codes "F11")}
                          {:keyCode (codes "F")
                           :altKey true}
                          {:keyCode (codes "E")
                           :altKey true}
                          {:keyCode (codes "A")
                           :ctrlKey true}
                          {:keyCode (codes "O")
                           :ctrlKey true}
                          {:keyCode (codes "S")
                           :ctrlKey true}
                          {:keyCode (codes "G")
                           :ctrlKey true}
                          {:keyCode (codes "P")
                           :ctrlKey true}
                          {:keyCode (codes "W")
                           :ctrlKey true}
                          {:keyCode (codes "K")
                           :ctrlKey true}
                          {:keyCode (codes "W")
                           :ctrlKey true}
                          {:keyCode (codes "D")
                           :ctrlKey true
                           :shiftKey true}]})
