(ns renderer.element.core
  (:require
   [re-frame.core :as rf]
   [renderer.action.events :as-alias action.events]
   [renderer.document.subs :as document.subs]
   [renderer.element.effects]
   [renderer.element.events :as element.events]
   [renderer.element.impl.core]
   [renderer.element.subs :as element.subs]
   [renderer.utils.key :as utils.key]))

(rf/dispatch [::action.events/register-action
              {:id :clipboard/cut
               :label [::element.events/cut]
               :icon "cut"
               :event [::element.events/cut]
               :shortcuts [{:keyCode (utils.key/codes "X")
                            :ctrlKey true}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :clipboard/copy
               :label [::copy "Copy"]
               :icon "copy"
               :event [::element.events/copy]
               :shortcuts [{:keyCode (utils.key/codes "C")
                            :ctrlKey true}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :clipboard/paste
               :label [::element.events/paste]
               :icon "paste"
               :event [::element.events/paste]
               :enabled [::document.subs/some-entities?]
               :shortcuts [{:keyCode (utils.key/codes "V")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :clipboard/paste-in-place
               :label [::element.events/paste-in-place]
               :icon "paste"
               :event [::element.events/paste-in-place]
               :enabled [::document.subs/some-entities?]
               :shortcuts [{:keyCode (utils.key/codes "V")
                            :ctrlKey true
                            :altKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :clipboard/paste-styles
               :label [::element.events/paste-styles]
               :icon "paste"
               :event [::element.events/paste-styles]
               :enabled [::element.subs/some-non-root-selected?]
               :shortcuts [{:keyCode (utils.key/codes "V")
                            :ctrlKey true
                            :shiftKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :element/duplicate
               :label [::element.events/duplicate]
               :icon "copy"
               :event [::element.events/duplicate]
               :shortcuts [{:keyCode (utils.key/codes "D")}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :element/delete
               :label [::element.events/delete]
               :icon "delete"
               :event [::element.events/delete]
               :shortcuts [{:keyCode (utils.key/codes "DELETE")}
                           {:keyCode (utils.key/codes "BACKSPACE")}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :element/select-all
               :label [::element.events/select-all]
               :icon "select-all"
               :event [::element.events/select-all]
               :enabled [::document.subs/some-entities?]
               :shortcuts [{:keyCode (utils.key/codes "A")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :element/deselect-all
               :label [::element.events/deselect-all]
               :icon "deselect-all"
               :event [::element.events/deselect-all]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :element/invert-selection
               :label [::element.events/invert-selection]
               :icon "invert-selection"
               :enabled [::document.subs/some-entities?]
               :event [::element.events/invert-selection]}])

(rf/dispatch [::action.events/register-action
              {:id :element/select-same-tags
               :label [::element.events/select-same-tags]
               :icon "select-same"
               :event [::element.events/select-same-tags]
               :shortcuts [{:keyCode (utils.key/codes "A")
                            :ctrlKey true
                            :shiftKey true}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/to-path
               :label [::element.events/object-to-path]
               :icon "bezier-curve"
               :event [::element.events/->path]
               :shortcuts [{:keyCode (utils.key/codes "P")
                            :ctrlKey true
                            :shiftKey true}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/stroke-to-path
               :label [::element.events/stroke-to-path]
               :icon "bezier-curve"
               :event [::element.events/stroke->path]
               :shortcuts [{:keyCode (utils.key/codes "P")
                            :ctrlKey true
                            :altKey true}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/group
               :label [::element.events/group]
               :icon "group"
               :event [::element.events/group]
               :shortcuts [{:keyCode (utils.key/codes "G")
                            :ctrlKey true}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/ungroup
               :label [::element.events/ungroup]
               :icon "ungroup"
               :event [::element.events/ungroup]
               :shortcuts [{:keyCode (utils.key/codes "G")
                            :ctrlKey true
                            :shiftKey true}]
               :enabled [::element.subs/some-selected-tag? :g]}])

(rf/dispatch [::action.events/register-action
              {:id :object/lock
               :label [::element.events/lock]
               :icon "lock"
               :event [::element.events/lock]
               :shortcuts [{:keyCode (utils.key/codes "L")
                            :ctrlKey true}]
               :enabled [::element.subs/some-selected-unlocked?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/unlock
               :label [::element.events/unlock]
               :icon "unlock"
               :event [::element.events/unlock]
               :shortcuts [{:keyCode (utils.key/codes "L")
                            :ctrlKey true
                            :shiftKey true}]
               :enabled [::element.subs/some-selected-locked?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/raise
               :label [::element.events/raise]
               :icon "bring-forward"
               :event [::element.events/raise]
               :shortcuts [{:keyCode (utils.key/codes "PAGE_UP")}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/lower
               :label [::element.events/lower]
               :icon "send-backward"
               :event [::element.events/lower]
               :shortcuts [{:keyCode (utils.key/codes "PAGE_DOWN")}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/raise-to-top
               :label [::element.events/raise-to-top]
               :icon "bring-front"
               :event [::element.events/raise-to-top]
               :shortcuts [{:keyCode (utils.key/codes "HOME")}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/lower-to-bottom
               :label [::element.events/lower-to-bottom]
               :icon "send-back"
               :event [::element.events/lower-to-bottom]
               :shortcuts [{:keyCode (utils.key/codes "END")}]
               :enabled [::element.subs/some-non-root-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/left
               :label [::element.events/align-left]
               :icon "objects-align-left"
               :event [::element.events/align-left]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/center-horizontal
               :label [::element.events/center-horizontally]
               :icon "objects-align-center-horizontal"
               :event [::element.events/center-horizontally]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/right
               :label [::element.events/align-right]
               :icon "objects-align-right"
               :event [::element.events/align-right]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/top
               :label [::element.events/align-top]
               :icon "objects-align-top"
               :event [::element.events/align-top]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/center-vertical
               :label [::element.events/center-vertically]
               :icon "objects-align-center-vertical"
               :event [::element.events/center-vertically]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/bottom
               :label [::element.events/align-bottom]
               :icon "objects-align-bottom"
               :event [::element.events/align-bottom]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :boolean/exclude
               :label [::element.events/boolean-exclude]
               :icon "exclude"
               :event [::element.events/boolean-exclude]
               :shortcuts [{:keyCode (utils.key/codes "E")
                            :ctrlKey true}]
               :enabled [::element.subs/multiple-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :boolean/unite
               :label [::element.events/boolean-unite]
               :icon "unite"
               :event [::element.events/boolean-unite]
               :shortcuts [{:keyCode (utils.key/codes "U")
                            :ctrlKey true}]
               :enabled [::element.subs/multiple-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :boolean/intersect
               :label [::element.events/boolean-intersect]
               :icon "intersect"
               :event [::element.events/boolean-intersect]
               :shortcuts [{:keyCode (utils.key/codes "I")
                            :ctrlKey true}]
               :enabled [::element.subs/multiple-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :boolean/subtract
               :label [::element.events/boolean-subtract]
               :icon "subtract"
               :event [::element.events/boolean-subtract]
               :shortcuts [{:keyCode (utils.key/codes "BACKSLASH")
                            :ctrlKey true}]
               :enabled [::element.subs/multiple-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :boolean/divide
               :label [::element.events/boolean-divide]
               :icon "divide"
               :event [::element.events/boolean-divide]
               :shortcuts [{:keyCode (utils.key/codes "D")
                            :ctrlKey true}]
               :enabled [::element.subs/multiple-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :animate/animate
               :label [::element.events/animate]
               :icon "animation"
               :event [::element.events/animate]
               :enabled [::element.subs/some-allow-content? :animate]}])

(rf/dispatch [::action.events/register-action
              {:id :animate/transform
               :label [::element.events/animate-transform]
               :icon "animation"
               :event [::element.events/animate-transform]
               :enabled [::element.subs/some-allow-content?
                         :animateTransform]}])

(rf/dispatch [::action.events/register-action
              {:id :animate/motion
               :label [::element.events/animate-motion]
               :icon "animation"
               :event [::element.events/animate-motion]
               :enabled [::element.subs/some-allow-content? :animateMotion]}])

(rf/dispatch [::action.events/register-action
              {:id :animate/add-mpath
               :label [::element.events/add-motion-path]
               :icon "bezier-curve"
               :event [::element.events/add-mpath]
               :enabled [::element.subs/some-selected-tag? :animateMotion]}])

(rf/dispatch [::action.events/register-action
              {:id :path/simplify
               :label [::element.events/path-simplify]
               :icon "bezier-curve"
               :event [::element.events/path-simplify]
               :enabled [::element.subs/some-selected-tag? :path]}])

(rf/dispatch [::action.events/register-action
              {:id :path/smooth
               :label [::element.events/path-smooth]
               :icon "bezier-curve"
               :event [::element.events/path-smooth]
               :enabled [::element.subs/some-selected-tag? :path]}])

(rf/dispatch [::action.events/register-action
              {:id :path/flatten
               :label [::element.events/path-flatten]
               :icon "bezier-curve"
               :event [::element.events/path-flatten]
               :enabled [::element.subs/some-selected-tag? :path]}])

(rf/dispatch [::action.events/register-action
              {:id :path/reverse
               :label [::element.events/path-reverse]
               :icon "bezier-curve"
               :event [::element.events/path-reverse]
               :enabled [::element.subs/some-selected-tag? :path]}])

(rf/dispatch [::action.events/register-action
              {:id :path/combine
               :label [::element.events/combine]
               :icon "group"
               :event [::element.events/combine]
               :enabled [::element.subs/some-selected-tag? :path]
               :shortcuts [{:keyCode (utils.key/codes "M")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :path/break-apart
               :label [::element.events/break-apart]
               :icon "ungroup"
               :event [::element.events/break-apart]
               :enabled [::element.subs/some-selected-tag? :path]
               :shortcuts [{:keyCode (utils.key/codes "M")
                            :ctrlKey true
                            :shiftKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :image/trace
               :label [::element.events/image-trace]
               :icon "image"
               :event [::element.events/trace]
               :enabled [::element.subs/some-selected-tag? :image]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/index-operations
               :icon "bring-forward"
               :label [::index-operations "Index operations"]
               :actions [:object/raise
                         :object/lower
                         :object/raise-to-top
                         :object/lower-to-bottom]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/boolean-operations
               :icon "subtract"
               :label [::boolean-operations "Boolean operations"]
               :enabled [::element.subs/multiple-selected?]
               :actions [:boolean/unite
                         :boolean/intersect
                         :boolean/subtract
                         :boolean/exclude
                         :boolean/divide]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/horizontal-alignment
               :icon "objects-align-left"
               :label [::horizontal-alignment "Horizontal alignment"]
               :actions [:align/left
                         :align/center-horizontal
                         :align/right]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/vertical-alignment
               :icon "objects-align-top"
               :label [::vertical-alignment "Vertical alignment"]
               :actions [:align/top
                         :align/center-vertical
                         :align/bottom]}])

(rf/dispatch [::action.events/register-action-group
              {:id :edit/clipboard
               :icon "copy"
               :label [::clipboard "Clipboard"]
               :actions [:clipboard/cut
                         :clipboard/copy
                         :clipboard/paste
                         :clipboard/paste-in-place
                         :clipboard/paste-styles]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/grouping
               :icon "group"
               :label [::grouping "Grouping"]
               :actions [:object/group
                         :object/ungroup]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/locking
               :icon "lock"
               :label [::locking "Locking"]
               :actions [:object/lock
                         :object/unlock]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/animate
               :icon "animation"
               :label [::element.events/animate]
               :enabled [::element.subs/some-non-root-selected?]
               :actions [:animate/animate
                         :animate/transform
                         :animate/motion
                         :animate/add-mpath]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/entity
               :icon "delete"
               :label [::entity "Entity"]
               :actions [:element/duplicate
                         :element/delete]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/path-operations
               :icon "bezier-curve"
               :label [::path "Path"]
               :enabled [::element.subs/some-selected-tag? :path]
               :actions [:path/simplify
                         :path/smooth
                         :path/flatten
                         :path/reverse
                         :path/combine
                         :path/break-apart]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/image-operations
               :icon "image"
               :label [::image "Image"]
               :enabled [::element.subs/some-selected-tag? :image]
               :actions [:image/trace]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/selection
               :icon "select-same"
               :label [::select "Select"]
               :actions [:element/select-all
                         :element/deselect-all
                         :element/invert-selection
                         :element/select-same-tags]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/convert
               :icon "swap-horizontal"
               :label [::convert "Convert"]
               :actions [:object/to-path
                         :object/stroke-to-path]}])
