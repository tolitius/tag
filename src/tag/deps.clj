(ns tag.deps
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.deps :as deps]
            [clojure.tools.deps.util.dir :as dirs]))

(defn- invert-deps
  "convert the :dependents info into a forward :deps map.

   `resolve-deps` returns a lib-map with :dependents (reverse dependencies),
   but `build-tree` needs forward dependencies (what each lib depends on).
   this function inverts the relationship.

   example:
   input lib-map: {org.clojure/clojure {:dependents [org.clojure/tools.deps]}}
   output deps-map: {org.clojure/tools.deps #{org.clojure/clojure}}"
  [lib-map]
  (reduce
    (fn [acc [lib {:keys [dependents]}]]
      (reduce
        (fn [acc dep]
          (update acc dep
                  (fnil conj #{})
                  lib))
        acc
        dependents))
    {}
    lib-map))

(defn- extract-version-from-path
  "extract the actual version from a jar path, checking if it's a snapshot based on -SNAPSHOT suffix.
   returns a map with :version and :snapshot? keys.
   example: '/path/foobar-0.3.2-20250311.201842-115.jar' with :mvn/version '0.3.2-SNAPSHOT'
            -> {:version '0.3.2-20250311.201842-115' :snapshot? true}"
  [lib-map lib path]
  (let [mvn-version (:mvn/version (get lib-map lib))
        snapshot? (and mvn-version (s/ends-with? mvn-version "-SNAPSHOT"))
        filename (last (s/split path #"/"))
        version-part (second (re-find #"(?i)^[^-]+-(.+)\.jar$" filename))]
    {:version (if snapshot? version-part mvn-version)
     :snapshot? snapshot?}))

(defn- build-tree
  "recursively build a dependency tree for the given library."
  [lib-map deps-map lib exclude-transitive]
  (let [lib-info (get lib-map lib)
        {:keys [version snapshot?]} (extract-version-from-path lib-map lib (first (:paths lib-info)))
        children (get deps-map lib)
        child-nodes (if (exclude-transitive lib)
                      "..."  ;; replace transitive deps with "..." if excluded
                      (when children
                        (mapv (fn [child-lib]
                                {child-lib (build-tree lib-map deps-map child-lib exclude-transitive)})
                              (sort children))))
        node (cond-> {:version version}
               snapshot?
               (assoc :snapshot? true)
               (contains? lib-info :exclusions)
               (assoc :status :excluded)
               child-nodes
               (assoc :depends-on child-nodes))]
    node))

(defn find-deps
  "collect dependencies information using clojure.tools.deps, including transitive deps.
   optionally takes a map with :exclude-transitive, a set of deps whose transitive dependencies are replaced with '...'.
   returns a map with :deps containing a nested dependency structure."
  ([] (find-deps {}))
  ([{:keys [exclude-transitive]}]
   (try
     (let [exclude-transitive (set (or exclude-transitive
                                       #{'org.clojure/tools.deps})) ;; exclude tools.deps by default, but can be overridden
           project-dir (str (dirs/canonicalize (io/file ".")))
           basis (deps/create-basis {:dir project-dir})
           lib-map (deps/resolve-deps basis nil)
           deps-map (invert-deps lib-map)
           direct-deps (-> basis :deps keys set)
           deps-tree (reduce (fn [acc lib]
                               (assoc acc lib (build-tree lib-map deps-map lib exclude-transitive)))
                             {}
                             (sort direct-deps))]
       {:deps deps-tree})
     (catch Exception e
       (println "could not load dependencies due to:" (.getMessage e))
       {:deps {}}))))
