(ns i18n-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.i18n.events :as-alias i18n.events]
   [renderer.i18n.subs :as-alias i18n.subs]
   [renderer.i18n.views :as i18n.views]))

(deftest language
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (testing "active language"
     (let [lang (rf/subscribe [::i18n.subs/user-lang])
           system-lang (rf/subscribe [::i18n.subs/system-lang])
           computed-lang (rf/subscribe [::i18n.subs/lang])]
       (testing "default"
         (is (= "system" @lang))
         (is (= "en-US" @system-lang))
         (is (= "en-US" @computed-lang)))

       (testing "set valid language"
         (rf/dispatch [::i18n.events/set-user-lang "el-GR"])
         (is (= "el-GR" @lang))
         (is (= "el-GR" @computed-lang)))))

   (testing "register language"
     (let [languages (rf/subscribe [::i18n.subs/languages])]
       (rf/dispatch [::i18n.events/register-language
                     {:id "im-LA"
                      :dir "ltr"
                      :locale "Imaginary language"
                      :code "LA"
                      :dictionary {}}])
       (is (contains? @languages "im-LA"))))

   (testing "set translation"
     (rf/dispatch [::i18n.events/set-user-lang "im-LA"])
     (rf/dispatch [::i18n.events/set-translation
                   "im-LA"
                   :renderer.menubar.views.file
                   "New File!"])
     (is (i18n.views/t [:renderer.menubar.views.file "New File!"])))

   (testing "deregister language"
     (let [languages (rf/subscribe [::i18n.subs/languages])]
       (rf/dispatch [::i18n.events/deregister-language "im-LA"])
       (is (not (contains? @languages "im-LA")))))))
