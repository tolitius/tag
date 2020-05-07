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
      (println (str "(!) could not run \"" cmd "\" => " rs ", but have no fear: I will continue without it."))
      out)))

(defn- scoop-git-intel []
  (-> {:commit-id (dosh "git rev-parse --short HEAD")
       :version/tag (dosh "git describe --abbrev=0")
       :repo-url (dosh "git config --get remote.origin.url")
       :commit-time (dosh "git log -1 --format=%cd")
       "commit human (or not)" (dosh "git log -1 --pretty=format:'%an'")
       :commit-message (dosh "git log -1 --pretty=%B")}
      (fmv #(some-> %
                    (s/replace #"\n" " ")
                    s/trim))))

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
   (export-intel intel {}))
  ([intel {:keys [app-name path filename]
           :or {filename "about.edn"
                app-name "noname"
                path "target/about/META-INF/"}}]
   (let [fpath (str path app-name "/")]
     (dosh (str "mkdir -p " fpath))
     (spit (str fpath filename)
           intel)
     {:intel-exported-to (str fpath filename)})))

(defn -main [& args]
  (when (< (count args) 2)
    (throw (ex-info "tag takes two params: 'app-name' and 'description'" {:args-passed args})))
  (let [[app-name & about] args]
    (-> (describe app-name {:about (->> about
                                        (interpose " ")
                                        (apply str))})
        (export-intel {:app-name app-name})
        println)
    (System/exit 0)))
