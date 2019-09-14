(ns den1k.defmostly
  (:require
   [den1k.defmostly.protocols :as p]
   [clojure.spec-alpha2 :as s])
  (:import (clojure.lang IFn)))

(declare spec-specificity)

(defn- spec-speci-dispatch [_ form]
  (when (seq? form)
    (first form)))

(defmulti spec-specificity* spec-speci-dispatch)

(defn- count-specs [specs] (transduce (map spec-specificity) + specs))

(defmethod spec-specificity* `s/keys
  [cnt form]
  (reduce (fn [out specs]
            (if (vector? specs)
              (+ out (count-specs specs))
              out))
          cnt
          form))

(defmethod spec-specificity* `s/coll-of
  [cnt [_ pred]]
  (+ cnt (spec-specificity pred)))

(defmethod spec-specificity* `s/schema
  [cnt [_ scm :as all]]
  (+ cnt (count-specs (vals (cond-> scm (vector? scm) first)))))

(defmethod spec-specificity* `s/select
  [cnt [_ schema selection]]
  (let [[_ [scm]] (s/form schema)]
    (+ cnt (count-specs (map #(get scm %) selection)))))

(defmethod spec-specificity* :default
  [cnt form]
  ;(println :default :cnt cnt)
  ;(println :default :form form)
  1)

(defn spec-specificity [spec]
  #_(when (or (keyword? spec) (s/spec? spec))
      (println
       (if (keyword? spec) spec)
       (s/form spec)))
  (cond->> spec
    (or (keyword? spec) (s/spec? spec)) s/form
    true (spec-specificity* 0)))

(defrecord MostlyMethod
  [ranked-spec-fns spec->fn]
  p/MultiFn
  (add-method [_ spec f]
    (swap! spec->fn assoc spec f)
    (swap! ranked-spec-fns conj {:spec spec
                                 :rank (spec-specificity spec)}))
  IFn
  (invoke [this args]
    (let [ret (reduce (fn [none {:keys [spec]}]
                        (if (s/valid? spec args)
                          (let [f (get @spec->fn spec)]
                            (reduced (f args)))
                          none))
                      ::none
                      @ranked-spec-fns)]
      (if (identical? ::none ret)
        (throw (ex-info (str "No method in mostlymethod '"
                             (-> this meta :name)
                             "' for dispatch value: " args)
                        {}))
        ret))))

(defn- ensure-vec [x]
  (cond
    (vector? x) x
    (nil? x) []
    (sequential? x) (vec x)
    :else [x]))

(defn- spec-comparable [spec]
  (ensure-vec (s/form spec)))

(defn- spec-compare [spec-x spec-y]
  (compare (spec-comparable spec-x) (spec-comparable spec-y)))

(defn- rank-comparator
  [{rank-x :rank spec-x :spec} {rank-y :rank spec-y :spec}]
  (let [c (compare rank-y rank-x)]
    (if (not= c 0)
      c
      (spec-compare spec-x spec-y))))

(defn mostly-method* []
  (->MostlyMethod (atom (sorted-set-by rank-comparator))
                  (atom {})))

(defmacro defmostly [name spec & fn-tail]
  `(do (defonce ~name (with-meta (mostly-method*) {:name '~name}))
       (p/add-method ~name
                     ~spec
                     (fn ~name ~@fn-tail))))
