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
               :label [::cut "Cut"]
               :icon "cut"
               :event [::element.events/cut]
               :shortcuts [{:keyCode (utils.key/codes "X")
                            :ctrlKey true}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :clipboard/copy
               :label [::copy "Copy"]
               :icon "copy"
               :event [::element.events/copy]
               :shortcuts [{:keyCode (utils.key/codes "C")
                            :ctrlKey true}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :clipboard/paste
               :label [::paste "Paste"]
               :icon "paste"
               :event [::element.events/paste]
               :enabled [::document.subs/entities?]
               :shortcuts [{:keyCode (utils.key/codes "V")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :clipboard/paste-in-place
               :label [::paste-in-place "Paste in place"]
               :icon "paste"
               :event [::element.events/paste-in-place]
               :enabled [::document.subs/entities?]
               :shortcuts [{:keyCode (utils.key/codes "V")
                            :ctrlKey true
                            :altKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :clipboard/paste-styles
               :label [::paste-styles "Paste styles"]
               :icon "paste"
               :event [::element.events/paste-styles]
               :enabled [::element.subs/some-selected?]
               :shortcuts [{:keyCode (utils.key/codes "V")
                            :ctrlKey true
                            :shiftKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :element/duplicate
               :label [::duplicate "Duplicate"]
               :icon "copy"
               :event [::element.events/duplicate]
               :shortcuts [{:keyCode (utils.key/codes "D")
                            :ctrlKey true}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :element/delete
               :label [::delete "Delete"]
               :icon "delete"
               :event [::element.events/delete]
               :shortcuts [{:keyCode (utils.key/codes "DELETE")}
                           {:keyCode (utils.key/codes "BACKSPACE")}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :element/select-all
               :label [::select-all "Select all"]
               :icon "select-all"
               :event [::element.events/select-all]
               :enabled [::document.subs/entities?]
               :shortcuts [{:keyCode (utils.key/codes "A")
                            :ctrlKey true}]}])

(rf/dispatch [::action.events/register-action
              {:id :element/deselect-all
               :label [::deselect-all "Deselect all"]
               :icon "deselect-all"
               :event [::element.events/deselect-all]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :element/invert-selection
               :label [::invert-selection "Invert selection"]
               :icon "invert-selection"
               :enabled [::document.subs/entities?]
               :event [::element.events/invert-selection]}])

(rf/dispatch [::action.events/register-action
              {:id :element/select-same-tags
               :label [::select-same-tags "Select same tags"]
               :icon "select-same"
               :event [::element.events/select-same-tags]
               :shortcuts [{:keyCode (utils.key/codes "A")
                            :ctrlKey true
                            :shiftKey true}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/to-path
               :label [::object-to-path "Object to path"]
               :icon "bezier-curve"
               :event [::element.events/->path]
               :shortcuts [{:keyCode (utils.key/codes "P")
                            :ctrlKey true
                            :shiftKey true}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/stroke-to-path
               :label [::stroke-to-path "Stroke to path"]
               :icon "bezier-curve"
               :event [::element.events/stroke->path]
               :shortcuts [{:keyCode (utils.key/codes "P")
                            :ctrlKey true
                            :altKey true}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/group
               :label [::group "Group"]
               :icon "group"
               :event [::element.events/group]
               :shortcuts [{:keyCode (utils.key/codes "G")
                            :ctrlKey true}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/ungroup
               :label [::ungroup "Ungroup"]
               :icon "ungroup"
               :event [::element.events/ungroup]
               :shortcuts [{:keyCode (utils.key/codes "G")
                            :ctrlKey true
                            :shiftKey true}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/lock
               :label [::lock "Lock"]
               :icon "lock"
               :event [::element.events/lock]
               :shortcuts [{:keyCode (utils.key/codes "L")
                            :ctrlKey true}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/unlock
               :label [::unlock "Unlock"]
               :icon "unlock"
               :event [::element.events/unlock]
               :shortcuts [{:keyCode (utils.key/codes "L")
                            :ctrlKey true
                            :shiftKey true}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/raise
               :label [::raise "Raise"]
               :icon "bring-forward"
               :event [::element.events/raise]
               :shortcuts [{:keyCode (utils.key/codes "PAGE_UP")}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/lower
               :label [::lower "Lower"]
               :icon "send-backward"
               :event [::element.events/lower]
               :shortcuts [{:keyCode (utils.key/codes "PAGE_DOWN")}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/raise-to-top
               :label [::raise-to-top "Raise to top"]
               :icon "bring-front"
               :event [::element.events/raise-to-top]
               :shortcuts [{:keyCode (utils.key/codes "HOME")}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :object/lower-to-bottom
               :label [::lower-to-bottom "Lower to bottom"]
               :icon "send-back"
               :event [::element.events/lower-to-bottom]
               :shortcuts [{:keyCode (utils.key/codes "END")}]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/left
               :label [::align-left "Align left"]
               :icon "objects-align-left"
               :event [::element.events/align :left]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/center-horizontal
               :label [::center-horizontally "Center horizontally"]
               :icon "objects-align-center-horizontal"
               :event [::element.events/align :center-horizontal]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/right
               :label [::align-right "Align right"]
               :icon "objects-align-right"
               :event [::element.events/align :right]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/top
               :label [::align-top "Align top"]
               :icon "objects-align-top"
               :event [::element.events/align :top]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/center-vertical
               :label [::center-vertically "Center vertically"]
               :icon "objects-align-center-vertical"
               :event [::element.events/align :center-vertical]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :align/bottom
               :label [::align-bottom "Align bottom"]
               :icon "objects-align-bottom"
               :event [::element.events/align :bottom]
               :enabled [::element.subs/not-every-top-level?]}])

(rf/dispatch [::action.events/register-action
              {:id :boolean/exclude
               :label [::boolean-exclude "Exclude"]
               :icon "exclude"
               :event [::element.events/boolean-operation :exclude]
               :shortcuts [{:keyCode (utils.key/codes "E")
                            :ctrlKey true}]
               :enabled [::element.subs/multiple-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :boolean/unite
               :label [::boolean-unite "Unite"]
               :icon "unite"
               :event [::element.events/boolean-operation :unite]
               :shortcuts [{:keyCode (utils.key/codes "U")
                            :ctrlKey true}]
               :enabled [::element.subs/multiple-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :boolean/intersect
               :label [::boolean-intersect "Intersect"]
               :icon "intersect"
               :event [::element.events/boolean-operation :intersect]
               :shortcuts [{:keyCode (utils.key/codes "I")
                            :ctrlKey true}]
               :enabled [::element.subs/multiple-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :boolean/subtract
               :label [::boolean-subtract "Subtract"]
               :icon "subtract"
               :event [::element.events/boolean-operation :subtract]
               :shortcuts [{:keyCode (utils.key/codes "BACKSLASH")
                            :ctrlKey true}]
               :enabled [::element.subs/multiple-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :boolean/divide
               :label [::boolean-divide "Divide"]
               :icon "divide"
               :event [::element.events/boolean-operation :divide]
               :shortcuts [{:keyCode (utils.key/codes "SLASH")
                            :ctrlKey true}]
               :enabled [::element.subs/multiple-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :animate/animate
               :label [::animate "Animate"]
               :icon "animation"
               :event [::element.events/animate :animate]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :animate/transform
               :label [::animate-transform "Animate Transform"]
               :icon "animation"
               :event [::element.events/animate :animateTransform]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :animate/motion
               :label [::animate-motion "Animate Motion"]
               :icon "animation"
               :event [::element.events/animate :animateMotion]
               :enabled [::element.subs/some-selected?]}])

(rf/dispatch [::action.events/register-action
              {:id :path/simplify
               :label [::path-simplify "Simplify"]
               :icon "bezier-curve"
               :event [::element.events/manipulate-path :simplify]
               :enabled [::element.subs/has-selected-tag? :path]}])

(rf/dispatch [::action.events/register-action
              {:id :path/smooth
               :label [::path-smooth "Smooth"]
               :icon "bezier-curve"
               :event [::element.events/manipulate-path :smooth]
               :enabled [::element.subs/has-selected-tag? :path]}])

(rf/dispatch [::action.events/register-action
              {:id :path/flatten
               :label [::path-flatten "Flatten"]
               :icon "bezier-curve"
               :event [::element.events/manipulate-path :flatten]
               :enabled [::element.subs/has-selected-tag? :path]}])

(rf/dispatch [::action.events/register-action
              {:id :path/reverse
               :label [::path-reverse "Reverse"]
               :icon "bezier-curve"
               :event [::element.events/manipulate-path :reverse]
               :enabled [::element.subs/has-selected-tag? :path]}])

(rf/dispatch [::action.events/register-action
              {:id :image/trace
               :label [::image-trace "Trace"]
               :icon "image"
               :event [::element.events/trace]
               :enabled [::element.subs/has-selected-tag? :image]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/index-operations
               :label [::index-operations "Index operations"]
               :actions [:object/raise
                         :object/lower
                         :object/raise-to-top
                         :object/lower-to-bottom]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/boolean-operations
               :label [::boolean-operations "Boolean operations"]
               :enabled [::element.subs/multiple-selected?]
               :actions [:boolean/unite
                         :boolean/intersect
                         :boolean/subtract
                         :boolean/exclude
                         :boolean/divide]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/horizontal-alignment
               :label [::horizontal-alignment "Horizontal alignment"]
               :actions [:align/left
                         :align/center-horizontal
                         :align/right]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/vertical-alignment
               :label [::vertical-alignment "Vertical alignment"]
               :actions [:align/top
                         :align/center-vertical
                         :align/bottom]}])

(rf/dispatch [::action.events/register-action-group
              {:id :edit/clipboard
               :label [::clipboard "Clipboard"]
               :actions [:clipboard/cut
                         :clipboard/copy
                         :clipboard/paste
                         :clipboard/paste-in-place
                         :clipboard/paste-styles]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/grouping
               :label [::grouping "Grouping"]
               :actions [:object/group
                         :object/ungroup]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/locking
               :label [::locking "Locking"]
               :actions [:object/lock
                         :object/unlock]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/animate
               :label [::animate "Animate"]
               :enabled [::element.subs/some-selected?]
               :actions [:animate/animate
                         :animate/transform
                         :animate/motion]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/entity
               :label [::entity "Entity"]
               :actions [:element/duplicate
                         :element/delete]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/path-operations
               :label [::path "Path"]
               :enabled [::element.subs/has-selected-tag? :path]
               :actions [:path/simplify
                         :path/smooth
                         :path/flatten
                         :path/reverse]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/image-operations
               :label [::image "Image"]
               :enabled [::element.subs/has-selected-tag? :image]
               :actions [:image/trace]}])

(rf/dispatch [::action.events/register-action-group
              {:id :object/selection
               :label [::select "Select"]
               :actions [:element/select-all
                         :element/deselect-all
                         :element/invert-selection
                         :element/select-same-tags]}])
