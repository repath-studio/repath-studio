(ns renderer.window.subs
  (:require
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]))

(rf/reg-sub
 ::window
 :-> :window)

(rf/reg-sub
 ::maximized?
 :<- [::window]
 :-> :maximized)

(rf/reg-sub
 ::fullscreen?
 :<- [::window]
 :-> :fullscreen)

(rf/reg-sub
 ::focused?
 :<- [::window]
 :-> :focused)

(rf/reg-sub
 ::width
 :<- [::window]
 :-> :width)

(rf/reg-sub
 ::breakpoint?
 :<- [::width]
 (fn [width [_ breakpoint]]
   ;; https://tailwindcss.com/docs/responsive-design#overview
   (>= width (get {:2xl 1536
                   :xl 1280
                   :lg 1024
                   :md 768
                   :sm 640} breakpoint))))

(rf/reg-sub
 ::xl?
 :<- [::breakpoint? :xl]
 :-> identity)

(rf/reg-sub
 ::md?
 :<- [::breakpoint? :md]
 :-> identity)

(rf/reg-sub
 ::sm?
 :<- [::breakpoint? :sm]
 :-> identity)

(rf/reg-sub
 ::window-controls?
 :<- [::app.subs/desktop?]
 :<- [::fullscreen?]
 :<- [::app.subs/mac?]
 (fn [[desktop? fullscreen? mac?] _]
   (and desktop? (not (or fullscreen? mac?)))))

(rf/reg-sub
 ::fullscreen-toggle?
 :<- [::fullscreen?]
 :<- [::app.subs/mac?]
 :<- [::app.subs/web?]
 :<- [::md?]
 (fn [[fullscreen? mac? web? md?] _]
   (or fullscreen? mac? (and web? md?))))

(rf/reg-sub
 ::app-icon?
 :<- [::fullscreen?]
 :<- [::app.subs/mac?]
 (fn [[fullscreen? mac?] _]
   (not (or fullscreen? mac?))))

(rf/reg-sub
 ::menubar?
 :<- [::md?]
 :<- [::app.subs/desktop?]
 (fn [[md? desktop?] _]
   (or md? desktop?)))
