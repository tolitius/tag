(ns tag.core
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.data.xml :as dx]
            [xml-in.core :as xml]
            [tag.shell :as sh]
            [tag.deps :as d]))

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

(defn pom? [pom]
  (or (.exists (io/file pom))
      (println "(!) could not find" pom "maven pom file, will skip maven intel")))

(defn- scoop-maven-intel [xpom]
  (when (pom? xpom)
    (let [pom (-> (slurp xpom)
                  (dx/parse-str :namespace-aware false))
          in-project (xml/find-first pom [:project])
          project (fn [k] (-> (xml/find-first in-project [k])
                              first))]
      {:group-id    (project :groupId)
       :artifact-id (project :artifactId)
       :version     (project :version)
       :name        (project :name)
       :description (project :description)
       :url         (project :url)})))

(defn- scoop-git-intel []
  (-> {:commit-id              (dosh "git rev-parse --short HEAD")
       :version/tag            (dosh "git describe --abbrev=0")
       :branch                 (dosh "git rev-parse --abbrev-ref HEAD")
       :repo-url               (dosh "git config --get remote.origin.url")
       :commit-time            (dosh "git log -1 --format=%cd")
       "commit human (or not)" (dosh "git log -1 --pretty=format:'%an'")
       :commit-message         (dosh "git log -1 --pretty=%B")}
      (fmv #(some-> %
                    (s/replace #"\n" " ")
                    s/trim))))

(defn describe
  ([app-name]
   (describe app-name {:about (str "..and " app-name " is my name")}))
  ([app-name {:keys [about]}]
   (let [maven (scoop-maven-intel "pom.xml")
         intel {:about
                {:app-name app-name
                 "what do I do?" about}
                :git (scoop-git-intel)
                :described-at (java.util.Date.)}]
     (if maven
       (assoc intel :maven maven)
       intel))))

(defn export-intel [app-name
                    {:keys [about deps exclude-transitive path]}]
  (let [fpath (str (or path "target/about/META-INF/")
                   app-name "/")
        to-about (str fpath "about.edn")
        to-deps (str fpath "deps.edn")]
    (dosh (str "mkdir -p " fpath))
    (spit to-about about)
    (spit to-deps deps)
    {:intel-exported-to {:about to-about
                         :deps to-deps}}))

(defn -main [& args]
  (when (< (count args) 2)
    (throw (ex-info "tag takes at least two params: 'app-name' and 'description'" {:args-passed args})))
  (let [[app-name description exclude-transitive] args]
    (->> {:about (describe app-name {:about description})
          :deps (d/find-deps {:exclude-transitive exclude-transitive})}
         (export-intel app-name)
         println)))
