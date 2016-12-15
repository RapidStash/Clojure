(ns rapidstash.core
  (:gen-class)
  (:require [cheshire.core :refer :all])
)

(def actions (list "insert","lookup","index"))
(def ops (list "#gt","#lt","#eq"))
(def the-data (list))

(defn in? 
  "true if coll contains elm"
  [coll elm]  
  (some #(= elm %) coll))

(defn display-data
  [data]
  (cond
    (> (count data) 0) (do
        (doseq [datum data]
        (println datum)
      )
    )
  )
  (println (count data) "results returned"))

(defn rs-insert
  "performs insert operation"
  [obj]
  (def the-data (conj the-data obj)))

(defn isOp?
  [op]
  (in? ops op))

(defn opMatchesDoc?
  [opAndVal docVal]
  (let [op (first opAndVal) clauseVal (second opAndVal)]
    (cond
      (not (instance? (type docVal) (clauseVal))) false
      (= op "#gt") (> (compare docVal clauseVal) 0)
      (= op "#lt") (< (compare docVal clauseVal) 0)
      (= op "#eq") (= (compare docVal clauseVal) 0)
      :else false)))

(defn is-map?
  [x]
  (or 
        (instance? clojure.lang.PersistentArrayMap x) 
        (instance? clojure.lang.PersistentVector$ChunkedSeq x)
  )
)

; recursively try to 
(defn matchesClause?
  [clause doc]
  (def clauseVal clause)
  (def docVal doc)
  
  ; idk
  (if (is-map? clauseVal)
    (def clauseVal (first (seq clauseVal)))
  )
  
  ; idk
  (if (is-map? docVal)
    (def docVal (first (seq docVal)))
  )
  
  ;
  (cond
    ; operator
    (isOp? (first clauseVal)) (opMatchesDoc? clauseVal docVal)
    ; non-matching attribute
    (not (= (first clauseVal) (first docVal))) false
    ; 
    :else (matchesClause? (second clauseVal) (second docVal))))

(defn rs-lookup
  "performs lookup operation"
  [conditions]
  (println "LOOKUP" conditions)
  (def temp-data the-data)

  "go over each condition and use it to filter the results"
  (let [condition (seq conditions)]
    (doseq [kv condition]
      (def temp-data (filter
        (fn [d]
          "we take this document and verify it against the condtion"
          (let [clauseKey (first kv) clauseVal (second kv) docVal (get d clauseKey)]
            (and
              (not (nil? clauseKey))
              (not (nil? clauseVal))
              (not (nil? docVal))
              (instance? (type docVal) clauseVal)
              (matchesClause? clauseVal docVal))))
        temp-data))))
  temp-data)

(defn rs-index
  "performs index operation"
  [obj]
  (println "INDEX" obj))

(defn trivial-validate
  "returns parsed json or nil if parsing fails or the parsed value is not the correct type"
  [in]
  (try
    (let [obj (parse-string in)]
      (if (instance? clojure.lang.PersistentArrayMap obj)
        obj
        nil))
    (catch Exception e nil)))

(defn get-actionname
  "returns the name of the action"
  [action]
  (nth action 0))

(defn get-action
  "returns the name of the action/command of the parsed json"
  [obj]
  (filter
    (fn [k]
       (in? actions (first k)))
    (seq obj)))

(defn valid-action?
  "returns true if the action/command is valid"
  [action]
  (if (in? actions (get-actionname action))
    true
    false))

(defn do-action
  "performs the action from the parsed json.  note: lazy validation of rest of obj"
  [action obj]
  (let [actionname (get-actionname action)]
    (cond
      (= actionname "insert") (rs-insert  (get obj actionname))
      (= actionname "lookup") (println (rs-lookup  (get obj actionname)))
      (= actionname "index")  (rs-index   (get obj actionname))
      :else nil)))

(defn continue-loop?
  "test to see if we need to continue"
  [msg]
  (and (not (nil? msg)) (not (= (clojure.string/lower-case msg) "exit"))))

(defn -main
  "entry point into console application"
  [& args]
  (print "rs ('exit' to quit)> ")
  (flush)
  (loop [reader (java.io.BufferedReader. *in*) ln (.readLine reader)]
    (if (continue-loop? ln)
      (do 
        ; perform action on the input and continue
        (let [obj (trivial-validate ln)]
          (if (not (nil? obj))
            (do
              (println "Input:")
              (println (generate-string obj {:pretty true}))
              ; perform the query
              (let [actions (get-action obj)]
                (doseq [action actions]
                  (if (valid-action? action)
                    (do-action action obj)
                    (println "Invalid action!")))))
            (println "Invalid statement!")))
        (print "rs> ")
        (flush)
        (recur reader (.readLine reader)))
      (println "Goodbye!"))))
