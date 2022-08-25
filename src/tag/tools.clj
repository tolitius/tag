(ns tag.tools
  (:require [clojure.string :as s]))

(defn key->prop [k]
  (-> k
      name
      (s/replace #"-| " "_")))

(defn link [connect from [to value]]
  (let [to (key->prop to)]
    [(str from connect to) value]))

(defn mapv->flat [m key->x connect]
  (reduce-kv (fn [path k v]
               (if (map? v)
                 (concat (map (partial link connect (key->x k))
                              (mapv->flat v key->x connect))
                         path)
                 (conj path [(key->x k) v])))
             [] m))

(defn map->flat
  "convers a nested map into one level
   with keys connected with the \"connect\" string

   e.g. (map->flat m \"_\")
  "
  [m connect]
  (->> (mapv->flat m key->prop connect)
       (into {})))

(defn map->txt [m]
  (->> (for [[k v] m]
         (str k " " v "\n"))
       (apply str)))

(defn about->txt [about]
  (-> about
      (map->flat "_")
      map->text))
