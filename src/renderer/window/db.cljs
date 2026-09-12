(ns renderer.window.db)

(def Breakpoint
  [:enum :2xl :xl :lg :md :sm])

(def Window
  [:map {:closed true}
   [:maximized {:default false} boolean?]
   [:minimized {:default false} boolean?]
   [:fullscreen {:default false} boolean?]
   [:focused {:default false} boolean?]
   [:visible {:default false} boolean?]
   [:width {:optional true} int?]])
