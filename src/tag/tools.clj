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

(defn map->prometheus [m]
  (as-> (for [[k v] m]
          (str k "=\"" v "\",")) xs    ;; make "k=v," pairs
        (apply str xs)
        (str "about_me{" xs)           ;; prefix with about_me{
        (s/replace xs #"\?|\(|\)" "")  ;; remove ? and ()
        (s/replace xs #",$" "}")))     ;; add last }

(defn about->prometheus
  "converts a map (from about.edn) to prometheus text based format:
   https://github.com/prometheus/docs/blob/main/content/docs/instrumenting/exposition_formats.md#text-based-format"
  [about]
  (if about
    (-> about
        (update-in [:git :commit-message]
                   s/replace #"\"" "'")   ;; double quotes to single within a commit message
        (map->flat "_")
        map->prometheus
        (str " 42"))                      ;; add prometheus counter
    {:error "Input value is missing"}))
