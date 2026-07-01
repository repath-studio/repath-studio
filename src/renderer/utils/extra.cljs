(ns renderer.utils.extra)

(defn rpartial
  "Like partial, takes a function f and fewer than the normal arguments to f,
   and returns a fn that takes a variable number of additional args.
   When called, the returned function calls f with the args prepended."
  ([f] f)
  ([f arg1]
   (fn
     ([] (f arg1))
     ([x] (f x arg1))
     ([x y] (f x y arg1))
     ([x y z] (f x y z arg1))
     ([x y z & args] (apply f x y z (concat args [arg1])))))
  ([f arg1 arg2]
   (fn
     ([] (f arg1 arg2))
     ([x] (f x arg1 arg2))
     ([x y] (f x y arg1 arg2))
     ([x y z] (f x y z arg1 arg2))
     ([x y z & args] (apply f x y z (concat args [arg1 arg2])))))
  ([f arg1 arg2 arg3]
   (fn
     ([] (f arg1 arg2 arg3))
     ([x] (f x arg1 arg2 arg3))
     ([x y] (f x y arg1 arg2 arg3))
     ([x y z] (f x y z arg1 arg2 arg3))
     ([x y z & args] (apply f x y z (concat args [arg1 arg2 arg3])))))
  ([f arg1 arg2 arg3 & more]
   (fn [& args] (apply f (concat args [arg1 arg2 arg3] more)))))
