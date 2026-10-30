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
          items (rf/subscribe [::shell.subs/items])
          current-text (rf/subscribe [::shell.subs/current-text])]

      (testing "initial"
        (is (= @active-language :js))
        (is (= @history [""]))
        (is (= @current-text "")))

      (testing "activate clojurescript"
        (rf/dispatch [::shell.events/activate-language :cljs])

        (rf.test/wait-for
         [::shell.events/language-load-success]

         (is (= @active-language :cljs))
         (is (= @history [""]))

         (testing "eval hashmap"
           (rf/dispatch [::shell.events/execute "{}"])

           (rf.test/wait-for
            [::shell.events/add-item]

            (is (= @history ["{}" ""]))
            (is (= (map :type @items) [:input :output]))
            (is (= (-> @items first :value :text) "{}"))
            (is (= (-> @items second :value) "{}"))

            (testing "set text"
              (rf/dispatch [::shell.events/set-text "foo"])

              (rf.test/wait-for
               [::shell.events/set-text]

               (is (= @current-text "foo"))

               (testing "clear output"
                 (rf/dispatch [::shell.events/clear-items])

                 (rf.test/wait-for
                  [::shell.events/clear-items]

                  (is (empty? @items))))))))))))))
