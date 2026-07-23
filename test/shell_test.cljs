(ns shell-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.shell.events :as-alias shell.events]
   [renderer.shell.subs :as-alias shell.subs]))

(deftest shell-languages
  (rf.test/run-test-async
   (rf/dispatch [::app.events/initialize])

   (rf.test/wait-for
    [::shell.events/language-load-success]

    (let [active-language (rf/subscribe [::shell.subs/active-language])
          history (rf/subscribe [::shell.subs/history])
          items (rf/subscribe [::shell.subs/items])]

      (testing "initial"
        (is (= @active-language :cljs))
        (is (= @history [""])))

      (testing "eval hashmap"
        (rf/dispatch [::shell.events/execute "{}"])

        (rf.test/wait-for
         [::shell.events/add-item]

         (is (= @history ["{}"]))
         (is (= @items ["{}"]))))

      (testing "clear command"
        (rf/dispatch [::shell.events/execute "(clear)"])

        (rf.test/wait-for
         [::shell.events/add-item]

         (is (= @history ["(clear)"]))
         (is (= @items []))))

      (testing "activate javascript"
        (rf/dispatch [::shell.events/activate-language :js])

        (rf.test/wait-for
         [::shell.events/language-load-success]

         (is (= @active-language :js))
         (is (= @history [""]))))))))

