(ns tag.shell
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh])
  (:import (java.io OutputStreamWriter ByteArrayOutputStream StringWriter)
           (java.nio.charset Charset)))

(defn sh
  "clojure.java.shell/sh relies on futures to retrieve shell results
   these futures are still alive at the end of the function which prevents -main / clojure scripts
   to exit. while (shutdown-agents) / system.exit would work they break other clojure parent
   processes (i.e. such as cursive repl and others)

   hence this 'sh' function is to prevent futures since tag does not need them."
  [& args]
  (let [[cmd opts] (#'clojure.java.shell/parse-args args)
        proc (.exec (Runtime/getRuntime)
                    ^"[Ljava.lang.String;" (into-array cmd)
                    (#'clojure.java.shell/as-env-strings (:env opts))
                    (io/as-file (:dir opts)))
        {:keys [out-enc]} opts]
    (.close (.getOutputStream proc))
    (with-open [stdout (.getInputStream proc)
                stderr (.getErrorStream proc)]
      (let [out (#'clojure.java.shell/stream-to-enc stdout out-enc)
            err (#'clojure.java.shell/stream-to-string stderr)
            exit-code (.waitFor proc)]
        {:exit exit-code :out out :err err}))))
