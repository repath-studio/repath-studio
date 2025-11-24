(ns document-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.document.db :as document.db]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.subs :as-alias element.subs]))

(deftest document
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [document-entities? (rf/subscribe [::document.subs/entities?])
         active-document (rf/subscribe [::document.subs/active])
         saved? (rf/subscribe [::document.subs/active-saved?])
         title-bar (rf/subscribe [::document.subs/title-bar])
         active-id (rf/subscribe [::document.subs/active-id])]
     (testing "defaults"
       (is @document-entities?)
       (is (document.db/valid? @active-document))
       (is (= "• Untitled-1 - Repath Studio" @title-bar)))

     (testing "close"
       (rf/dispatch [::document.events/close @active-id false])
       (is (not @active-document)))

     (testing "close active"
       (rf/dispatch [::document.events/new])
       (rf/dispatch [::document.events/saved false {:id (:id @active-document)
                                                    :title "File-1"}])
       (rf/dispatch [::document.events/close-active])
       (is (not @active-document)))

     (testing "close saved"
       (rf/dispatch [::document.events/new])
       (rf/dispatch [::document.events/new])
       (rf/dispatch [::document.events/saved false {:id (:id @active-document)
                                                    :title "File-1"}])
       (rf/dispatch [::document.events/close-saved])
       (is (= (count @(rf/subscribe [::document.subs/entities])) 1)))

     (testing "close all"
       (rf/dispatch [::document.events/saved false {:id (:id @active-document)
                                                    :title "File-2"}])
       (rf/dispatch [::document.events/close-all])
       (is (not @active-document)))

     (testing "create"
       (rf/dispatch [::document.events/new])
       (is (= "• Untitled-1 - Repath Studio" @title-bar))

       (rf/dispatch [::document.events/new-from-template [800 600]])
       (is (= "• Untitled-2 - Repath Studio" @title-bar))
       (is (= "800" (->> @(rf/subscribe [::element.subs/entities])
                         (filter #(= (:tag %) :svg))
                         (first)
                         :attrs
                         :width))))

     (testing "colors"
       (let [fill (rf/subscribe [::document.subs/fill])
             stroke (rf/subscribe [::document.subs/stroke])]
         (testing "default color values"
           (is (= @fill "white"))
           (is (= @stroke "black")))

         (testing "swap colors"
           (rf/dispatch [::document.events/swap-colors])
           (is (= @fill "black"))
           (is (= @stroke "white")))

         (testing "set fill"
           (rf/dispatch [::document.events/set-attr :fill "red"])
           (is (= @fill "red")))

         (testing "set stroke"
           (rf/dispatch [::document.events/set-attr :stroke "yellow"])
           (is (= @stroke "yellow")))))

     (testing "collapse/expand elements"
       (let [collapsed-ids (rf/subscribe [::document.subs/collapsed-ids])
             id (random-uuid)]
         (testing "default state"
           (is (empty? @collapsed-ids)))

         (testing "collapse"
           (rf/dispatch [::document.events/collapse-el id])
           (is (= #{id} @collapsed-ids)))

         (testing "expand"
           (rf/dispatch [::document.events/expand-el id])
           (is (empty? @collapsed-ids)))))

     (testing "hover elements"
       (let [hovered-ids (rf/subscribe [::document.subs/hovered-ids])
             id (random-uuid)]
         (testing "default state"
           (is (empty? @hovered-ids)))

         (testing "hover"
           (rf/dispatch [::document.events/set-hovered-id id])
           (is (= #{id} @hovered-ids)))

         (testing "clear hovered"
           (rf/dispatch [::document.events/clear-hovered])
           (is (empty? @hovered-ids)))))

     (testing "save"
       (testing "default state"
         (is (not @saved?)))

       (testing "save"
         (rf/dispatch [::document.events/saved false {:id (:id @active-document)
                                                      :title "File-1"}])
         (is @saved?)
         (is @(rf/subscribe [::document.subs/saved? (:id @active-document)]))))

     (testing "load"
       (rf/dispatch [::document.events/load
                     {:version "100000.0.0" ; Skips migrations.
                      :path "foo/bar/document.rps"
                      :title "document.rps"
                      :elements {}}])

       (is @(rf/subscribe [::document.subs/active-saved?]))
       (is (= "foo/bar/document.rps - Repath Studio" @title-bar)))

     (testing "load multiple"
       (let [recent-documents (rf/subscribe [::document.subs/recent])]
         (rf/dispatch [::document.events/load-multiple
                       [{:version "100000.0.0"
                         :id #uuid "58c1f2bb-94ea-4b95-b042-c42121384b9a"
                         :path "foo/bar/document-1.rps"
                         :title "document-1.rps"
                         :elements {}}
                        {:version "100000.0.0"
                         :id #uuid "01eafc60-3ce3-41d2-a138-1fe1759ad4f2"
                         :path "foo/bar/document-2.rps"
                         :title "document-2.rps"
                         :elements {}}]])

         (is (= (:title @active-document) "document-2.rps"))
         (is (= (take 2 @recent-documents)
                [{:id #uuid "01eafc60-3ce3-41d2-a138-1fe1759ad4f2"
                  :title "document-2.rps"
                  :path "foo/bar/document-2.rps"}
                 {:id #uuid "58c1f2bb-94ea-4b95-b042-c42121384b9a"
                  :title "document-1.rps"
                  :path "foo/bar/document-1.rps"}])))))))
