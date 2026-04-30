(ns renderer.worker.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :worker
 :-> :worker)

(rf/reg-sub
 ::tasks
 :<- [:worker]
 :-> :tasks)

(rf/reg-sub
 ::some-active?
 :<- [::tasks]
 :-> (comp boolean seq))
