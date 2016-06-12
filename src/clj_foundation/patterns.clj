(ns clj-foundation.patterns
  (:require [schema.core :as s :refer [=> =>*]]
            [potemkin :refer [def-map-type]])
  (:gen-class))


;; Schema/type utilities ------------------------------------------------------------------------------------

(defn types
  "Returns a schema that matches the listed surface types only (e.g.: primitive or collection types but
  not contents)."
  [& schemas]
  (let [type-names (map #(.getName %) schemas) ;; FIXME: This only works for schemas that are (instance? java.lang.Class)
        type-name-string (apply str (interpose ", " type-names))]
    (s/named (apply s/cond-pre schemas) type-name-string)))


(s/defn get-package :- s/Str
  "Returns the package name for the specified Class"
  [clazz :- Class]
  (->> (.split (.getName clazz) "\\.")
       reverse
       rest
       reverse
       (interpose ".")
       (apply str)))


(s/defn get-class-name :- s/Str
  "Returns the unqualified class name for the specified Class"
  [clazz :- Class]
  (->> (.split (.getName clazz) "\\.")
       reverse
       first
       (apply str)))


;; The singleton pattern ------------------------------------------------------------------------------------

(defmacro def-singleton-fn
  "Define a function whose return value is initilized once by executing body and that returns
  that same value on every subsequent call.  A Clojure implementation of the Singleton pattern."
  [name & body]
  `(def ~name (memoize (fn [] ~@body))))


;; The Nothing object ---------------------------------------------------------------------------------------


(def-map-type Nothing []
  (get [_ k default-value]
       default-value)
  (assoc [_ k v] (assoc {} k v))
  (dissoc [_ k] {})
  (keys [_] nil)
  (meta [_] nil)
  (with-meta [this mta] this)
  (toString [this] "Nothing"))


(def nothing
  "Nothing is the value to use when there is nothing to pass or return.  Note that Nothing
  acts like an empty map.  This has the following implications:

  * You can use it as a result in mapcat when you want nothing appended to the output collection.
  * You can cons a value into nothing, resulting in a seq.
  * You can assoc values into a nothing, resulting in a map.
  * You can conj vector pairs into a nothing, resulting in a map.
  * etc..."
  (Nothing.))


(s/defn Nothing! :- Class
  "Return the Nothing type (mainly for use in Schemas)"
  []
  Nothing)


(s/defn something? :- s/Any
  "Returns value if value is not nothing; else returns nil."
  [value :- s/Any]
  (if (instance? Nothing value)
    nil
    value))


(s/defn nothing->identity :- s/Any
  "Takes nil or nothing to the specified identity value for the type and computation in context,
  otherwise returns value.  An identity value can be applied to a value of the given type under the
  operation in context without affecting the result.  For example 0 is the identity value for rational
  numbers under addition.  The empty string is the identity value for strings under concatination.

  Note that Nothing is already an identity value for maps and seqs.  This function is only useful
  for types where the Nothing type is ill-behaved (e.g.: Strings, Numbers, ...) for a given operation.

  Another name for this concept is the monadic zero for the type/operation."
  [identity-value :- s/Any, value :- s/Any]

  (if (something? value)
    value
    identity-value))


;; Retain intermediate steps in a map -----------------------------------------------------------------------

(defmacro let-map
  "A version of let that returns its local variables in a map.
  If a result is computed in the body, and that result is another map,
  let-map returns the result of conj-ing the result map into the let
  expression map.  Otherwise it returns a vector containing the let
  expression  map followed by the result."
  [var-exprs & body]
  (let [vars (map (fn [[var form]] [(keyword var) var]) (partition 2 var-exprs))
        has-body (not (empty? body))]
    `(let [~@var-exprs
           result# (do ~@body)
           mapvars# (into {} [~@vars])]
       (if ~has-body
         (if (map? result#)
           (conj mapvars# result#)
           [mapvars# result#])
         mapvars#))))


(defmacro letfn-map
  "A version of letfn that returns its functions in a map.
  If a result is computed in the body, and that result is another map,
  fn-map returns the result of conj-ing the result map into the function
  map.  Otherwise it returns a vector containing the function map
  followed by the result."
  [fn-exprs & body]
  (let [fn-refs (map (fn [f] [(keyword (first f)) (first f)]) fn-exprs)
        has-body (not (empty? body))]
    `(letfn [~@fn-exprs]
       (let [result# (do ~@body)
             mapfns# (into {} [~@fn-refs])]
         (if ~has-body
           (if (map? result#)
             (conj mapfns# result#)
             [mapfns# result#])
           mapfns#)))))
