(ns subs.core
  "A collection of sane validation functions for maps which are:
   * validations are composable, just take two maps and deep-merge them
   * No macros, all pure functions and ds 
   * Simple to extend
  Assumptions:
   * Predicates should be able to handle nil, there is no ordering in how they are invoked
   * You map define a :pre entry that defines when to activate a check
  "
 )

(defn- deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn- deep-merge-with
  "Like merge-with, but merges maps recursively, applying the given fn
  only when there's a non-map at a particular level.

  (deepmerge + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
               {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
  -> {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}"
  [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))

(defn flatten-keys*
  "Flatten map keys into vectors, used to calculate fast access vectors from descriptions"
  [a ks m]
  (if (map? m)
    (reduce into (map (fn [[k v]] (flatten-keys* a (conj ks k) v)) (seq m)))
    (assoc a ks m)))

(def ^:private flatten-mem (memoize flatten-keys*))

(defn flatten-keys 
  "memoized version that spares us the need to calculate the access vector each time"
  [m] (flatten-mem {} [] m))

(def not-nil (comp not nil?))

(defmacro when-not-nil 
  "returns an fn that applies body on pred if values isn't nil else nil"
  [pred & body]
  `(fn [v#] 
     (when (and (not-nil v#) (not (~pred v#)))
       ~@body
       )))

(defmacro when*
  "returns an fn that applies body on pred else nil"
  [pred & body]
  `(fn [v#] 
     (when (~pred v#)
       ~@body
       )))

(def ^:private base {
  :String (when-not-nil string? "must be a string")                     
  :Integer  (when-not-nil integer? "must be a integer")                     
  :Vector  (when-not-nil vector? "must be a vector")                     
  :Map  (when-not-nil map? "must be a map")                     
  :Set  (when-not-nil set? "must be a set")                     
  :Keyword  (when-not-nil keyword? "must be a keyword")                     
  :sequential  (when-not-nil sequential? "must be sequential")                     
  :required  (when* nil? "must be present")                     
  })

(def ^:private externals (atom {}))

(defn- run-vs 
  "Runs a set of validations on value" 
  [value vs] 
  {:pre [(set? vs)]}
  (let [merged (merge base @externals)]
    (filter identity 
        (map #((merged %) value) vs))))

(defn validate! 
  "validates a map with given validations" 
  [m validations & opts]
  (reduce 
    (fn [errors [k vs]] 
      (let [e (run-vs (get-in m k) vs)]
        (if (seq e) (assoc-in errors k e) errors))) {} (flatten-keys validations)))

(defn validation 
  "define an custom validation with type and predicate"
  [type pred]
  (swap! externals assoc type pred))

(defn combine 
  "Combines a seq of validation descriptions"
  [& ds]
  (apply deep-merge-with clojure.set/union ds))

(comment 
  (validation :level  (when-not-nil #{:info :debug :error} "must be either info debug or error"))
  (def m1 {:machine {:ip #{:String :required} :names #{:Vector}} :vcenter {:pool #{:String}}}) 
  (def m2 {:machine {:ip #{:String :required} :names #{:required} :level #{:level}}}) 
  (validate! {:machine {:names {:foo 1} :ip 1}} (combine m1 m2) :error ::non-valid-machine)
  ) 