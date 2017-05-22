(ns clj-foundation.fn-spec
  "Support for making function specs DRYer to write."
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :refer [prewalk-replace]]))


(defmacro resolved
  "Returns the resolved var for sym; i.e.: sym -> #'sym"
  [sym]
  (let [v (resolve sym)]
    `~v))


(defn args
  "Return f's argument lists.  Public for testability only."
  [f]
  (-> f meta :arglists))


(defn validations
  "public for testability only"
  [symbols specs symbols-str]
  (assert (= (count symbols) (count specs))
          (str "(count arguments) != (count specs): " symbols-str "/" specs))

  (let [spec-symbol-pairs (partition 2 (interleave specs symbols))]
    (map (fn [[spec symbol]] `(s/valid? ~spec ~symbol)) spec-symbol-pairs)))


(defn- symbol->spec [symbols specs]
  (apply assoc {} (interleave symbols specs)))


(defn spec? [a] (or (keyword? a) (instance? clojure.lang.IFn a)))


(defmacro =>
  "Define a typed function.  The function's type signature is first, followed by the
  usual defn parameters.  The metadata map is supplied by this macro and cannot currently
  be overridden/supplemented.

  Define function f with calls to s/valid for each parameter and the return value in the
  :pre and :post conditions of f.  Only handles a single argument list.  Includes
  the derived function type in the docstring.

  f               - The function to define.
  parameter-specs - A vector of the specs to use to validate each (destructured) argument.
  return-spec     - A spec to validate the return value.
  body            - docstring? [arglist-vector] & statements"
  [f parameter-specs return-spec & more]

  {:pre [(s/valid? symbol? f)
         (s/valid? (s/coll-of spec?) parameter-specs)
         (s/valid? spec? return-spec)
         (s/valid? #(not (empty? %)) more)]}

  (let [[docstring
         arglist
         body]         (if (string? (first more))
         [(first more) (second more) (rest (rest more))]
         [""           (first more)  (rest more)])
        arglist-str    (pr-str arglist)
        arglist-vector (flatten arglist)
        argspec        (prewalk-replace (symbol->spec arglist-vector parameter-specs) arglist)
        all-valid?     (validations arglist-vector parameter-specs arglist-str)
        type-str       (str "(=> " argspec " " return-spec ")")
        typed-docs     (str (if (empty? docstring) "" (str docstring "\n")) type-str)]

    (assert (s/valid? (s/coll-of spec?) arglist))

    `(defn ~f ~typed-docs ~arglist
       {:pre  [~@all-valid?]
        :post [(s/valid? ~return-spec ~(symbol "%"))]}
       ~@body)))
