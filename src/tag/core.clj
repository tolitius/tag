(ns tag.core
  (:require [clojure.string :as s]
            [clojure.java.shell :as sh]))

(defn- fmv
  "apply f to each value v of map m"
  [m f]
  (into {}
        (for [[k v] m]
          [k (f v)])))

(defn- dosh [cmd]
  (let [with-args (s/split cmd #" ")
        {:keys [out exit error] :as rs} (apply sh/sh with-args)]
    (if-not (zero? exit)
      (println (str "(!) could not run \"" cmd "\" => " rs))
      out)))

(defn- scoop-git-intel []
  (-> {:commit-id (dosh "git rev-parse --short HEAD")
       :version/tag (dosh "git describe --abbrev=0")
       :commit-time (dosh "git log -1 --format=%cd")
       "commit human (or not)" (dosh "git log -1 --pretty=format:'%an'")
       :commit-message (dosh "git log -1 --pretty=%B")}
      (fmv (comp s/trim
                 #(s/replace % #"\n" " ")))))

(defn describe
  ([app-name]
   (describe app-name {:about (str "..and " app-name " is my name")}))
  ([app-name {:keys [about]}]
   {:about
    {:app-name app-name
     "what do I do?" about}
    :git (scoop-git-intel)
    :described-at (java.util.Date.)}))

(defn export-intel
  ([intel]
   (export-intel intel {:path "target/META-INF"}))
  ([intel {:keys [path]
           :or {path "target/META-INF"}}]
   (dosh (str "mkdir -p " path))
   (spit (str path "/about.edn")
         intel)))
