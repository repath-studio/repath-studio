(ns renderer.utils.extra)

(defn rpartial
  "Like partial, takes a function f and fewer than the normal arguments to f,
   and returns a fn that takes a variable number of additional args.
   When called, the returned function calls f with the args prepended."
  [f & bound-args]
  (fn [& args]
    (apply f (concat args bound-args))))
