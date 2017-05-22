(ns clj-foundation.fn-spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
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
   "Happy case test function"
   [[a b] c]

   (str (* a b) " " c))


(=> sad [number? number? string?] string?
   "Wrong return type"
   [[a b] c]

   (* a b))


(deftest =>-test
  (testing "The function's docstring includes user-defined docstring and the function type."
    (let [docstring (-> #'f meta :doc)]
      (is (str/includes? docstring "Happy case test function"))
      (is (str/includes? docstring "(=> [[number? number?] string?] string?)"))))

  (testing "Calling function functions correctly"
    (is (= "6 times" (f [2 3] "times"))))

  (testing "Incorrect argument types or return value types throw an assertion error"
    (is (thrown? AssertionError (f ["" 3] "fold")))
    (is (thrown? AssertionError (f [2 ""] "fold")))
    (is (thrown? AssertionError (f [2 3] 42)))

    (is (thrown? AssertionError (sad [2 3] "fold")))))
