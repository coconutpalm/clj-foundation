(ns clj-foundation.fn-spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clj-foundation.fn-spec :refer :all]))


(defmacro literal [code] `~code)

(s/def ::word-vector (s/coll-of string?))


(defn id
  "An identity function for test data"
  ([val] val)
  ([v1 v2] [v1 v2]))


(deftest resolved-test
  (testing "returns the resolved symbol"
    (is (= #'id (resolved id)))))


(deftest args-test
  (testing "Returns function's argument list(s)"
    (is (= [['val] ['v1 'v2]] (args (resolved id))))))


(deftest validations-test
  (testing "(count symbols) must be the same as (count spcs)"
    (is (thrown? AssertionError (validations ['a 'b] [string? string? string?] "[a b]"))))

  (testing "Generates validations for each symbol/spec pair"
    (let [a 'a
          b 'b]
      (is (= `((s/valid? ~number? ~a)
               (s/valid? ~string? ~b))
             (validations [a b] [number? string?] "[a b]"))))))


(=> f [number? number? string?] string?
   "docstring"
   [[a b] c]

   (str (* a b) " " c))
