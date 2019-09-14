(ns den1k.defmostly.meander
  (:require
   [den1k.defmostly.protocols :as p]
   [meander.match.delta :as mm])
  (:import (clojure.lang IFn)))


;; initial thoughts
;; https://github.com/noprompt/meander/issues/65

(defn defmostly1* []
  (let [patterns->fns (atom {})]
    (fn [pattern f]
      (swap! patterns->fns assoc pattern f)
      (fn [& args]
        (doseq [[pat f] @patterns->fns] ; exhaustive
          (when (apply pat args)
            (apply f args)))))
    )
  )



(defrecord MostlyMethod [patterns->fns]
  p/MultiFn
  (add-method [_ pattern f]
    (swap! patterns->fns assoc pattern f))
  IFn
  (invoke [_ args]
    (doseq [[pat f] @patterns->fns]     ; exhaustive
      (when (pat args)
        (f args)))))

(defn mostly-method* []
  (->MostlyMethod (atom {})))

(defmacro defmostly [name pattern & fn-tail]
  `(do (defonce ~name (mostly-method*))
       (p/add-method ~name
                     (fn [x#]
                       (try
                         (list 'mm/match x# '~pattern true)
                         (catch Exception e#
                           nil)))
                     (fn ~@fn-tail))))

(comment
 (defmostly abc
            [?a ?b ?a]
            ([a] (prn :abc a)))



 (defmostly abc {:name (pred some? ?name) :title (pred some? ?title)}
            ([x]
             (print x)
             :yoo))

 (abc [1 2 1])
 (abc {:name "Mike Foe"}))

