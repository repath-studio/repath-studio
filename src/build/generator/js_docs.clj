(ns build.generator.js-docs
  "Shadow-cljs build hook that runs the Node script
   (src/build/generator/js-docs.mjs) extracting the JS built-in docs from the
   pinned TypeScript lib declarations. The script generates the
   generated.js-docs CLJS data namespace (src/generated/js_docs.cljs), which
   the renderer shell requires directly (see renderer.shell.reepl.sci), so no
   doc text is written by hand.

   The hook runs at the `:configure` and `:compile-prepare` stages, i.e.
   before compilation, and the Node script only rewrites the file when its
   content changes.")

(defn generate!
  []
  (let [cmd (into-array String ["node" "src/build/generator/js-docs.mjs"])
        builder (doto (ProcessBuilder. cmd) (.inheritIO))
        process (.start builder)]
    (when-not (zero? (.waitFor process))
      (throw (Exception. "js docs generator failed")))))

(defn ^:export hook!
  {:shadow.build/stages #{:configure :compile-prepare}}
  [build-state & _args]
  (case (:shadow.build/stage build-state)
    :configure (do (generate!)
                   build-state)
    :compile-prepare (do (generate!)
                         build-state)))
