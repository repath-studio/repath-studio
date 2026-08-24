(ns renderer.timeline.db)

(def Timeline
  [:map {:closed true}
   [:time {:default 0} number?]
   [:speed {:default 1} number?]
   [:replay {:default false} boolean?]
   [:grid-snap {:default true} boolean?]
   [:guide-snap {:default true} boolean?]
   [:fit-duration {:default true} boolean?]
   [:paused {:default true} boolean?]])
