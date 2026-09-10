(ns renderer.shell.views
  (:require
   ["@codemirror/autocomplete" :refer [closeBrackets]]
   ["@codemirror/language" :refer [bracketMatching]]
   ["@codemirror/view" :refer [EditorView]]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.action.views :as action.views]
   [renderer.events :as-alias events]
   [renderer.i18n.views :as i18n.views]
   [renderer.panel.events :as-alias panel.events]
   [renderer.panel.subs :as-alias panel.subs]
   [renderer.panel.views :as panel.views]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.hierarchy :as shell.hierarchy]
   [renderer.shell.reepl.sci :as shell.reepl.sci]
   [renderer.shell.reepl.show-devtools :as show-devtools]
   [renderer.shell.reepl.show-function :as show-function]
   [renderer.shell.reepl.show-value :refer [show-value]]
   [renderer.shell.subs :as-alias shell.subs]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.utils.codemirror :as utils.codemirror]
   [renderer.utils.dom :as utils.dom]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn should-eval?
  [inst evt]
  (or (.-metaKey evt)
      (and (not (.-shiftKey evt))
           (utils.codemirror/in-place? inst))))

(defn on-keyup-handler
  [options evt inst]
  (.stopPropagation evt)
  (case (.-key evt)
    "Escape"
    (rf/dispatch (if @(:completion options)
                   [::shell.events/clear-completion]
                   [::events/blur]))

    "Enter"
    (rf/dispatch [::shell.events/clear-completion])

    ("Control" "Alt" "Meta" "ContextMenu")
    (rf/dispatch [::shell.events/set-show-all-completions false])

    (when-not (contains? #{"Tab" "Shift"} (.-key evt))
      (rf/dispatch [::shell.events/complete-word inst]))))

(defn on-keydown-handler
  [options evt inst]
  (let [{:keys [on-eval on-up on-down should-cycle]} options]
    (.stopPropagation evt)
    (case (.-key evt)
      ("Control" "Alt" "Meta" "ContextMenu")
      (rf/dispatch [::shell.events/set-show-all-completions true])

      "Tab"
      (when should-cycle
        (.preventDefault evt)
        (rf/dispatch [::shell.events/cycle-completions (.-shiftKey evt)]))

      "Enter"
      (when (should-eval? inst evt)
        (.preventDefault evt)
        (on-eval (.. inst -state -doc toString)))

      "ArrowUp"
      (when (and (not (.-shiftKey evt))
                 (utils.codemirror/first-line? inst))
        (.preventDefault evt)
        (on-up))

      "ArrowDown"
      (when (and (not (.-shiftKey evt))
                 (utils.codemirror/last-line? inst))
        (.preventDefault evt)
        (on-down))

      nil)))

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
  [views/cm-editor value
   {:theme-mode (:theme-mode options)
    :extensions (conj [(bracketMatching)
                       (closeBrackets)
                       (EditorView.contentAttributes.of
                        #js {:id utils.dom/shell-input-id
                             :aria-label "Shell"})]
                      (:extensions options))
    :on-blur #(rf/dispatch [::shell.events/clear-completion])
    :on-change (:on-change options)
    :on-keyup (partial on-keyup-handler options)
    :on-keydown (partial on-keydown-handler options)}])

(defn repl-input
  []
  (let [lang @(rf/subscribe [::shell.subs/active-language])
        theme-mode @(rf/subscribe [::theme.subs/computed-mode])
        cycle? @(rf/subscribe [::shell.subs/cycle-completions?])
        completion (rf/subscribe [::shell.subs/completion])
        repl-history? @(rf/subscribe [::panel.subs/visible? :repl-history])
        loaded? @(rf/subscribe [::shell.subs/language-loaded?])
        current-text @(rf/subscribe [::shell.subs/current-text])]
    [:div.flex.items-center
     [:div.flex.self-start.flex-1
      [:div.flex.text-xs.self-start
       {:class "p-1.5 pr-1"}
       (if loaded?
         (str (shell.reepl.sci/current-ns) "=>")
         [:span.text-foreground-muted
          (i18n.views/t [::loading-language "Loading language..."])])]
      [:div.flex-1.py-px
       (when loaded?
         ^{:key lang}
         [code-mirror current-text
          (merge {:theme-mode theme-mode
                  :should-cycle cycle?
                  :completion completion
                  :on-eval #(rf/dispatch [::shell.events/execute %])
                  :on-change #(rf/dispatch [::shell.events/set-text
                                            (.. % -state -doc toString)])
                  :on-up #(rf/dispatch [::shell.events/go-up])
                  :on-down #(rf/dispatch [::shell.events/go-down])}
                 (shell.hierarchy/codemirror-options lang))])]]
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

(defn- url
  [s]
  [:button.text-info.cursor-pointer.underline
   {:on-click #(rf/dispatch [::events/open-remote-url s])}
   s])

(defn- command
  [s {:keys [theme-mode language]}]
  [views/static-highlight s theme-mode (shell.hierarchy/parser language)
   {:class "inline cursor-pointer"
    :on-click #(rf/dispatch [::shell.events/set-text s])}])

(defmulti item (fn [i _opts] (:type i)))

(defmethod item :input
  [{{:keys [current-ns text]} :value} {:keys [theme-mode language]}]
  [:div.flex.gap-2
   [:div.text-foreground-muted.font-bold (str current-ns "=>")]
   [views/static-highlight text theme-mode
    (shell.hierarchy/parser language)
    {:class "flex-1 cursor-pointer break-words cursor-pointer"
     :on-click #(rf/dispatch [::shell.events/set-text text])}]])

(defmethod item :error
  [{:keys [value]} opts]
  [:div.text-error.select-text
   (shell.hierarchy/show-error (:language opts) value)])

(defmethod item :output
  [{:keys [value]} opts]
  [:div.flex-1.break-words.select-text
   [show-value value nil opts]])

(defmethod item :info
  [{:keys [value]} opts]
  (->> value
       (map (fn [segment]
              (if (vector? segment)
                (let [[protocol text] segment]
                  (case protocol
                    :url (url text)
                    :command (command text opts)
                    :else (str segment)))
                (str segment))))
       (into [:div.flex-1.break-words.select-text])))

(defn maybe-fn-docs
  [f]
  (let [doc (shell.reepl.sci/doc-from-sym f)]
    (when (:forms doc)
      (with-out-str
        (shell.reepl.sci/print-doc doc)))))

(defn repl-items
  []
  (let [loaded? @(rf/subscribe [::shell.subs/language-loaded?])
        items @(rf/subscribe [::shell.subs/items])
        theme-mode @(rf/subscribe [::theme.subs/computed-mode])
        lang @(rf/subscribe [::shell.subs/active-language])
        md? @(rf/subscribe [::window.subs/md?])
        opts {:theme-mode theme-mode
              :language lang
              :showers [show-devtools/show-devtools
                        (partial show-function/show-fn-with-docs
                                 maybe-fn-docs)]}]
    [:div.flex-1.h-full.overflow-hidden.flex.flex-col
     [views/toolbar
      {:class "bg-primary"}
      [views/action-icon-button :shell/clear-output]
      [views/action-switch :shell/toggle-verbose]
      [:div.grow]
      [:div.flex-1]
      (when md? [panel.views/close-button :repl-history])]
     [:div.flex.flex-1.h-full.overflow-hidden.border-b..border-border
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
  [text selected active index]
  [:div.p-1.bg-secondary.text-nowrap.hover:bg-primary
   {:ref #(when selected (rf/dispatch [::events/scroll-into-view %]))
    :on-pointer-down #(do (.preventDefault %)
                          (rf/dispatch [::shell.events/activate-completion
                                        index]))
    :class (when selected (if active
                            "bg-accent! text-accent-foreground!"
                            "bg-primary!"))}
   text])

(defn function-docs
  [s]
  (let [theme-mode @(rf/subscribe [::theme.subs/computed-mode])
        lang @(rf/subscribe [::shell.subs/active-language])
        lines (string/split-lines s)
        signature (when (seq (nth lines 2 nil)) (nth lines 2 nil))
        doc (string/join "\n" (drop-while string/blank? (drop 3 lines)))]
    [:div.bg-primary.drop-shadow.p-4.absolute.bottom-full.flex.flex-col.gap-4
     [:div.font-semibold.text-normal.text-sm
      [views/static-highlight (str (first lines)) theme-mode
       (shell.hierarchy/parser lang)]]
     (when (seq signature)
       [views/static-highlight signature theme-mode
        (shell.hierarchy/parser lang)])
     (when (seq doc) [:div doc])]))

(defn completion-list
  []
  (let [words @(rf/subscribe [::shell.subs/completion-words])
        active? @(rf/subscribe [::shell.subs/completion-active?])
        show-all? @(rf/subscribe [::shell.subs/completion-show-all?])
        pos @(rf/subscribe [::shell.subs/completion-pos])
        docs @(rf/subscribe [::shell.subs/docs])]
    [:div#completion-list.absolute.bottom-full.left-0.w-full.text-xs.mb-px
     (when docs [function-docs docs])
     (->> words
          (map-indexed (fn [index word]
                         [completion-item
                          (second word)
                          (= index pos)
                          active?
                          index]))
          (into [:div.overflow-hidden.flex
                 {:class (when show-all? "flex-wrap")}]))]))

(defn root
  []
  (let [repl-history? @(rf/subscribe [::panel.subs/visible? :repl-history])
        md? @(rf/subscribe [::window.subs/md?])]
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
      [completion-list]
      [repl-input]]]))
