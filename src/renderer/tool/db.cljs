(ns renderer.tool.db
  (:require
   [renderer.db :refer [Vec2]]
   [renderer.element.db :refer [Element ElementId]]
   [renderer.hierarchy :as hierarchy]
   [renderer.i18n.db :refer [Translation]]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(defn tool?
  [tool]
  (hierarchy/gisa? tool ::tool.hierarchy/tool))

(def Tool
  [:fn {:error/fn (fn [{:keys [value]} _]
                    (str value " is not a supported tool"))}
   tool?])

(def State
  [:enum :idle :translate :clone :scale :select :create :edit
   :type :pan :rotate])

(def Cursor
  [:enum
   "auto"
   "default"
   "none"
   "context-menu"
   "help"
   "pointer"
   "progress"
   "wait"
   "cell"
   "crosshair"
   "text"
   "vertical-text"
   "alias"
   "copy"
   "move"
   "no-drop"
   "not-allowed"
   "grab"
   "grabbing"
   "e-resize"
   "n-resize"
   "ne-resize"
   "nw-resize"
   "s-resize"
   "se-resize"
   "sw-resize"
   "w-resize"
   "ew-resize"
   "ns-resize"
   "nesw-resize"
   "nwse-resize"
   "col-resize"
   "row-resize"
   "all-scroll"
   "zoom-in"
   "zoom-out"])

(def HandleAction
  [:enum :translate :scale :rotate :edit])

(def HandleId keyword?)

(def Handle
  [:map {:closed true}
   [:id HandleId]
   [:label {:optional true} Translation]
   [:action HandleAction]
   [:type [:= :handle]]
   [:rounded {:optional true} boolean?]
   [:implied {:optional true} boolean?]
   [:cursor {:optional true} Cursor]
   [:position {:optional true} Vec2]
   [:size {:optional true} number?]
   [:stroke-width {:optional true} number?]
   [:parent {:optional true} ElementId]])

(def ToolHistoryState [:map-of ElementId Element])

(def ToolHistory
  [:map {:closed true}
   [:undos [:vector ToolHistoryState]]
   [:redos [:vector ToolHistoryState]]])
