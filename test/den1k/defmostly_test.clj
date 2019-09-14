(ns den1k.defmostly-test
  (:require [clojure.test :refer :all]
            [den1k.defmostly :refer :all]
            [clojure.spec-alpha2 :as s]
            [den1k.defmostly.protocols :as p])
  (:import (java.util UUID)))

(s/def ::id uuid?)
(s/def ::email string?)
(s/def ::title string?)
(s/def ::body string?)
(s/def ::user (s/keys :req-un [::id ::email]))
(s/def ::author ::user)
(s/def ::comment (s/keys :req-un [::id ::author ::body]))
(s/def ::comments (s/coll-of ::comment))
(s/def ::blogpost (s/keys :req-un [::author ::id] :opt-un [::title ::body ::comments]))

(s/def ::author-schema (s/schema {:id ::id :email ::email}))
(s/def ::blogpost-schema
  (s/schema {:id       uuid?
             :title    ::title
             :body     ::body
             :comments (s/coll-of (s/schema {:id     uuid?
                                             :author ::author-schema
                                             :body   ::body}))
             :author   ::author-schema}))


(deftest spec-specificity-test
  (are [cnt spec] (= cnt (spec-specificity spec))
    1 ::id
    2 ::user
    4 ::comment
    4 ::comments
    9 ::blogpost

    2 ::author-schema
    9 ::blogpost-schema
    7 (s/select ::blogpost-schema [:id :title :body :comments])))

(deftest defmostly-test
  (let [mm       (doto (mostly-method*)
                   (p/add-method ::id (fn [id] [:an-id id]))
                   (p/add-method ::user (fn [user] [:a-user user]))
                   (p/add-method ::comments
                                 (fn [comments] [:comments comments]))
                   (p/add-method (s/select ::author-schema [:id])
                                 (fn [user] [:an-author user]))
                   (p/add-method (s/select ::blogpost-schema [:id :title])
                                 (fn [bp] [:a-blogpost bp])))

        mock-id  (UUID/randomUUID)
        user     {:id    mock-id
                  :email "user@defmostly.com"}
        comments [{:id mock-id :body "abc" :author user}]
        author   (dissoc user :email)
        blogpost {:id mock-id :title "abc" :body "bpbody"}]
    (are [exp arg] (= exp (mm arg))
      [:an-id mock-id] mock-id
      [:a-user user] user
      [:comments comments] comments

      [:an-author author] author
      [:a-blogpost blogpost] blogpost)
    (is (thrown? Exception (mm :no-match)))
    (p/add-method mm (s/spec any?) (fn [x] [:default x]))
    (is (= [:default :no-match] (mm :no-match)))))

