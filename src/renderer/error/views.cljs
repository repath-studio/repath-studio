(ns renderer.error.views
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.events :as-alias events]
   [renderer.history.events :as-alias history.events]
   [renderer.views :as views]
   [renderer.window.events :as-alias window.events]))

(m/=> submit-error-url [:-> string? string?])
(defn submit-error-url
  [message]
  (str "https://github.com/repath-studio/repath-studio/issues/new?"
       "&title=" message
       "&template=bug_report.md"))

(defn message
  [error]
  (let [error-message (if (string? @error) @error (.-message @error))
        stack (when-not (string? @error) (.-stack @error))]
    [:div.flex.max-h-dvh.overflow-hidden
     [views/scroll-area
      [:div.flex.w-full.justify-center.items-center
       [:div.w-full.max-w-xl.bg-primary.p-8.md:m-8.m-2
        [:div.text-xl.pr-10.pb-5 "The following unhandled error was thrown"]

        [:p.text-error error-message]
        (when stack
          [:details.mb-5
           [:summary "Stacktrace"]
           [:pre.border.mt-2.p-2.border-border.text-wrap.text-2xs.select-text
            {:class "overflow-hidden"}
            stack]])

        [:p "Please consider submitting an error report to improve your
             experience."]

        [:button.button.px-2.rounded.w-full.mb-5.border.border-border
         {:on-click #(rf/dispatch [::events/open-remote-url
                                   (submit-error-url error-message)])}
         "Submit an error report"]

        [:p "You can try to undo your last action in order to recover to a
             previous working state."]

        [:button.button.px-2.rounded.w-full.mb-5.border.border-border
         {:on-click #(do (rf/dispatch [::history.events/undo])
                         (reset! error nil))}
         "Undo your last action"]

        [:p "If undoing did't work, you can try restarting the application."]

        [:button.button.px-2.rounded.w-full.mb-5.border.border-border
         {:on-click #(rf/dispatch [::window.events/relaunch])}
         "Restart the application"]

        [:p "If you keep getting the same error after restarting, try clearing
             your session and options data and restart. Please note that by
             doing so, you will loose any unsaved changes and your local
             application settings."]

        [:button.button.px-2.rounded.w-full.border.border-border.text-error!
         {:on-click #(rf/dispatch [::window.events/clear-data-and-relaunch])}
         "Clear data and restart"]]]]]))

(defn boundary
  "Catching rendering errors with an error boundary.
   https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary"
  []
  (let [error (reagent/atom nil)]
    (reagent/create-class
     {;; Try to revert to a working state
      ;; https://react.dev/reference/react/Component#static-getderivedstatefromerror
      :get-derived-state-from-error
      #(reset! error %)

      :reagent-render
      (fn [children]
        (if @error
          [message error]
          children))})))
