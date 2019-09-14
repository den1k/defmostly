# defmostly

Multimethod-like dispatch leveraging Clojure Spec.

Clojure's multimethods (`defmulti`) require the dispatch function to produce
an exact match for one of the registered `defmethod`s.

`defmostly` takes a looser approach that levarages specs and their specificity.

Specificity.. What is it? Currently a recursive count of spec predecates.
For example `(s/def ::user (s/keys :req-un [::id ::email]))` has a specificity
of 2.

During definition `defmostly` determines the specificity of the passed in spec. 

During dispatch `defmostly` reduces over the specs, narrowest (highest specificity)
first, running `s/valid?` with passed argument. Upon the first valid result, it
shortcircuits and invokes the argument with the registered function.

`defmostly` is an idea-sketch and should not be used in production. 

## Usage

```clojure
(ns my-ns
  (:require [den1k.defmostly :refer [defmostly]]))
  
(defmostly auth ::logged-out-user
  [user]
  (println :login! user))

(defmostly auth ::logged-in-user
  [user]
  (println :logout! user))

(auth {:db/id #uuid "84a4afff-8816-401f-a09e-8d4325801f86"})

;=> :login! #:db{:id #uuid "84a4afff-8816-401f-a09e-8d4325801f86"}

(auth {:db/id #uuid "84a4afff-8816-401f-a09e-8d4325801f86"
       :user/session {:session/key "foo"}})

;=> :logout! {:db/id #uuid "712fcc62-77e6-4811-86f9-6db8528f69eb"
;             :user/session #:session{:key foo}}
```

## Todo
- CLJC compatability
- benchmarks & performance improvements 

## License

Copyright © 2019 Dennis Heihoff

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
