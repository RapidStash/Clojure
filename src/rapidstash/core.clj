(ns rapidstash.core
  (:gen-class)
  (:require [cheshire.core :refer :all]))

(def actions (list "insert","lookup","index"))

(defn rs-insert
  "performs insert operation"
  [obj]
  (println "INSERT" obj))

(defn rs-lookup
  "performs lookup operation"
  [obj]
  (println "LOOKUP" obj))

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

(defn get-action
  "returns the db action/command of the parsed json"
  [obj]
  (clojure.string/lower-case (nth (nth (seq obj) 0) 0)))

(defn get-actionvalue
  "returns the value of the action passed in"
  [obj]
  (nth (nth (seq obj) 0) 1))

(defn valid-action?
  "returns true if the action/command is valid"
  [obj]
  (let [action (get-action obj)]
    (if (in? actions (get-action obj))
      true
      false)))

(defn do-action
  "performs the action from the parsed json.  note: lazy validation of rest of obj"
  [obj]
  (let [action (get-action obj) actionval (get-actionvalue obj)]
    (cond
      (= action "insert") (rs-insert actionval)
      (= action "lookup") (rs-lookup actionval)
      (= action "index") (rs-index actionval)
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
              (if (valid-action? obj)
                (do-action obj)
                (println "Invalid action!")))
            (println "Invalid statement!")))
        (print "rs> ")
        (flush)
        (recur (read-line)))
      (println "Goodbye!"))))
