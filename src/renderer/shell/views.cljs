(ns renderer.shell.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["codemirror" :as codemirror]
   ["codemirror/addon/edit/closebrackets.js"]
   ["codemirror/addon/edit/matchbrackets.js"]
   ["codemirror/addon/hint/show-hint.js"]
   ["codemirror/addon/runmode/colorize.js"]
   ["codemirror/addon/runmode/runmode.js"]
   ["react" :as react]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.action.views :as action.views]
   [renderer.events :as-alias events]
   [renderer.i18n.views :as i18n.views]
   [renderer.panel.events :as-alias panel.events]
   [renderer.panel.subs :as-alias panel.subs]
   [renderer.panel.views :as panel.views]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.codemirror :as shell.reepl.codemirror]
   [renderer.shell.reepl.replumb :as reepl.replumb]
   [renderer.shell.reepl.show-devtools :as show-devtools]
   [renderer.shell.reepl.show-function :as show-function]
   [renderer.shell.reepl.show-value :refer [show-value]]
   [renderer.shell.subs :as-alias shell.subs]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.utils.dom :as utils.dom]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]
   [replumb.core :as replumb])
  (:require-macros
   [reagent.ratom :refer [reaction]]))

(defn color-highlighted-text
  [_text _theme]
  (let [ref (react/createRef)
        colorize #(when-let [dom-el (.-current ref)]
                    ((aget codemirror "colorize") #js[dom-el] "clojure")
                    ;; Hacky way to remove the theme class added by CodeMirror
                    ;; https://codemirror.net/5/addon/runmode/colorize.js
                    (-> dom-el .-classList (.remove "cm-s-default")))]
    (reagent/create-class
     {:component-did-mount
      (fn [_this] (colorize))

      :component-did-update
      (fn [_this _old-argv] (colorize))

      :reagent-render
      (fn [text theme]
        [:pre.p-0.m-0
         {:class (str "cm-s-" theme)
          :ref ref}
         text])})))

(defn language-dropdown-button
  [enabled?]
  (let [active-language @(rf/subscribe [::shell.subs/active-language])
        action-group (action.views/deref-action-group :shell/languages)
        {:keys [actions label]} action-group]
    [:> DropdownMenu/Root
     [:> DropdownMenu/Trigger
      {:as-child true}
      [:button.form-control-button.font-mono.px-2!.bg-transparent!
       {:title (i18n.views/t label)
        :disabled (not enabled?)}
       (string/upper-case (name active-language))]]
     [:> DropdownMenu/Portal
      (->> actions
           (map views/dropdown-menu-item)
           (into [:> DropdownMenu/Content
                  {:side "top"
                   :align "end"
                   :class "menu-content rounded-sm"
                   :on-key-down #(.stopPropagation %)
                   :on-escape-key-down #(.stopPropagation %)}
                  [views/dropdownmenu-arrow]]))]]))

(defn code-mirror
  [value options]
  [views/cm-editor
   value
   {:props {:id utils.dom/shell-input-id
            :style {:height "auto"
                    :flex 1}}
    :options (merge {:viewportMargin js/Infinity
                     :extraKeys #js {"Shift-Enter" "newlineAndIndent"}
                     :value value
                     :keyMap "default"
                     :showCursorWhenSelecting true
                     :screenReaderLabel "Shell"}
                    (:cm-options options))
    :on-blur #(reset! (:complete-atom options) nil)
    :on-keyup (partial shell.reepl.codemirror/on-keyup-handler options)
    :on-keydown (partial shell.reepl.codemirror/on-keydown-handler options)}])

(defn repl-input
  [complete-atom]
  (let [lang @(rf/subscribe [::shell.subs/active-language])
        codemirror-theme @(rf/subscribe [::theme.subs/codemirror])
        repl-history? @(rf/subscribe [::panel.subs/visible? :repl-history])
        loaded? @(rf/subscribe [::shell.subs/language-loaded?])
        current-text @(rf/subscribe [::shell.subs/current-text])]
    [:div.flex.items-center
     [:div.flex.self-start.p-1.5.flex-1
      [:div.flex.text-xs.self-start
       {:class "p-0.5"}
       (if loaded?
         (string/trim (replumb/get-prompt))
         [:span.text-foreground-disabled
          (i18n.views/t [::loading-language "Loading language..."])])]
      [:div.flex-1
       {:class "p-0.5"}
       (when loaded?
         [code-mirror current-text
          {:on-eval #(rf/dispatch [::shell.events/execute %])
           :on-change #(rf/dispatch [::shell.events/set-text %])
           :complete-word #(shell.hierarchy/completions lang %)
           :on-up #(rf/dispatch [::shell.events/go-up])
           :on-down #(rf/dispatch [::shell.events/go-down])
           :complete-atom complete-atom
           :cm-options (merge {:theme codemirror-theme}
                              (shell.hierarchy/codemirror-options lang))}])]]
     [:div.self-start.h-full.flex.items-center
      [language-dropdown-button loaded?]
      (when @(rf/subscribe [::window.subs/md?])
        [:div.self-start.flex
         [:button.form-control-button.bg-transparent!
          {:title (i18n.views/t
                   (if repl-history?
                     [::hide-command-output "Hide command output"]
                     [::show-command-output "Show command output"]))
           :on-click #(rf/dispatch [::panel.events/toggle :repl-history])}
          [views/icon (if repl-history? "chevron-down" "chevron-up")]]])]]))

(defmulti item (fn [i _opts] (:type i)))

(defmethod item :input
  [{{:keys [current-ns text]} :value} {:keys [theme]}]
  [:div.flex.gap-2
   [:div.text-foreground-disabled.font-bold (str current-ns "=>")]
   [:div.flex-1.cursor-pointer.break-words
    {:on-click #(rf/dispatch [::shell.events/set-text text])}
    [color-highlighted-text text theme]]])

(defmethod item :error
  [{:keys [value]} opts]
  [:div.text-error.select-text
   (shell.hierarchy/show-error (:language opts) value)])

(defmethod item :output
  [{:keys [value]} opts]
  [:div.flex-1.break-words.select-text
   [show-value value nil opts]])

(defn maybe-fn-docs
  [f]
  (let [doc (reepl.replumb/doc-from-sym f)]
    (when (:forms doc)
      (with-out-str
        (reepl.replumb/print-doc doc)))))

(defn repl-items
  []
  (let [loaded? @(rf/subscribe [::shell.subs/language-loaded?])
        items @(rf/subscribe [::shell.subs/items])
        codemirror-theme @(rf/subscribe [::theme.subs/codemirror])
        lang @(rf/subscribe [::shell.subs/active-language])
        verbose? @(rf/subscribe [::shell.subs/verbose?])
        md? @(rf/subscribe [::window.subs/md?])
        opts {:theme codemirror-theme
              :language lang
              :showers [show-devtools/show-devtools
                        (partial show-function/show-fn-with-docs
                                 maybe-fn-docs)]}]
    [:div.flex-1.h-full.overflow-hidden.flex.flex-col
     [views/toolbar
      [views/icon-button
       "delete"
       {:title (i18n.views/t [::clear-output-history "Clear output history"])
        :on-click #(rf/dispatch [::shell.events/clear-items])}]
      [views/switch
       (i18n.views/t [::verbose-output "Verbose output"])
       {:id "verbose"
        :default-checked verbose?
        :on-checked-change #(rf/dispatch [::shell.events/set-verbose %])}]
      [:div.flex-1]
      (when md? [panel.views/close-button :repl-history])]
     [:div.flex.flex-1.h-full.overflow-hidden.border-b.border-t.border-border
      (if loaded?
        [views/scroll-area
         {:ref #(rf/dispatch [::events/scroll-to-bottom %])}
         (->> items
              (map (fn [i]
                     [:div.font-mono.p-1.flex.text-xs.min-h-4 [item i opts]]))
              (into [:div.p-1 {:dir "ltr"}]))]
        [:div.flex.items-center.justify-center.h-full.w-full
         [views/loading-indicator]])]]))

(defn completion-item
  [text selected active set-active]
  [:div.p-1.bg-secondary.text-nowrap
   {:ref #(when selected (rf/dispatch [::events/scroll-into-view %]))
    :on-pointer-enter set-active
    :class (when selected (if active
                            "bg-accent! text-accent-foreground!"
                            "bg-primary!"))}
   text])

(defn function-docs
  [s]
  (let [codemirror-theme @(rf/subscribe [::theme.subs/codemirror])
        [fn-name signature doc] (filter seq (string/split-lines s))]
    [:div.bg-primary.drop-shadow.p-4.absolute.bottom-full.flex.flex-col.gap-4
     [:div.font-semibold fn-name]
     (when signature [color-highlighted-text signature codemirror-theme])
     (when doc [:div doc])]))

(defn completion-list
  [docs {:keys [pos words active show-all]} set-active]
  (let [items (map-indexed #(vector completion-item
                                    (get %2 2)
                                    (= %1 pos)
                                    active
                                    (partial set-active %1)) words)]
    [:div#completion-list.absolute.bottom-full.left-0.w-full.text-xs.mb-px
     (when docs
       [function-docs docs])
     (into
      [:div.overflow-hidden.flex
       {:class (when show-all "flex-wrap")}]
      items)]))

(defn docs-reaction
  [lang complete-atom]
  (reaction
   (when-let [state @complete-atom]
     (let [{:keys [pos words]} state
           sym (first (get words pos))]
       (shell.hierarchy/docs lang sym)))))

(defn root
  []
  (let [repl-history? @(rf/subscribe [::panel.subs/visible? :repl-history])
        md? @(rf/subscribe [::window.subs/md?])
        lang @(rf/subscribe [::shell.subs/active-language])]
    (reagent/with-let [complete-atom (reagent/atom nil)
                       docs (docs-reaction lang complete-atom)]
      [:<>
       (if md?
         (when repl-history?
           [panel.views/panel
            {:id :repl-history
             :class "relative"
             :minSize 100
             :defaultSize 300}
            [repl-items]])
         [repl-items])

       [:div.relative.whitespace-pre-wrap.font-mono.w-full
        {:dir "ltr"}
        [completion-list
         @docs
         @complete-atom
         #(swap! complete-atom assoc :pos % :active true)]
        [repl-input complete-atom]]])))
