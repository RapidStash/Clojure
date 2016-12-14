(ns rapidstash.core
  (:gen-class)
  (:require [cheshire.core :refer :all])
)

(use 'clojure.set)

(def actions (list "insert","lookup","index"))

(def the-data (list))

(defn rs-insert
  "performs insert operation"
  [obj]
  (println "INSERT" obj)
  (def the-data (conj the-data obj))
  (println "data: " the-data))

(defn rs-lookup
  "performs lookup operation"
  [obj]
  (println "LOOKUP" obj)
  (def temp-data the-data)
  (let [s (seq obj)]
    (doseq [kv s]
      (def temp-data (filter
        (fn [d]
          ; TODO: this only works for non-nested documents.  Needs extra logic for recursive comparison of arrays/maps
          (== (get d (first kv)) (second kv)))
        temp-data))))
  (println temp-data))


(defn rs-index
  "performs index operation"
  [obj]
  (println "INDEX" obj))

(defn in? 
  "true if coll contains elm"
  [coll elm]  
  (some #(= elm %) coll))

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
      (= actionname "lookup") (rs-lookup  (get obj actionname))
      (= actionname "index")  (rs-index   (get obj actionname))
      :else nil)))

(defn -main
  "entry point into console application"
  [& args]
  (print "rs ('exit' to quit)> ")
  (flush)
  (loop [ln (read-line)]
    (if (not (= (clojure.string/lower-case ln) "exit"))
      (do
        "perform action on the input and continue"
        (let [obj (trivial-validate ln)]
          (if (not (nil? obj))
            (do
              (println "You entered:")
              (println (generate-string obj {:pretty true}))
              (let [actions (get-action obj)]
                (doseq [action actions]
                  (if (valid-action? action)
                    (do-action action obj)
                    (println "Invalid action!")))))
            (println "Invalid statement!")))
        (print "rs> ")
        (flush)
        (recur (read-line)))
      (println "Goodbye!"))))
