(ns com.xaidat.workshopstats
  (:require [clojure-csv.core :as csv]
            [clojure.tools.cli :as cli]
            [clojure.data.json :as json]
            [cheshire.core :as cson])
  ; TODO: Creating standalone tools (1/2)
  (:gen-class)

  (:import (jdk.internal.org.objectweb.asm.tree.analysis Analyzer)))


; ----------------------------------------------------------------------------------------------------------------------
; dealing with questions list construction and file IO

(defn csv-question-classifier [in-map q]
  "Returns the in-map expanded by an entry for q: If q's answers should be split up as CSV (marked by leading !), q will
   map to true (with the leading ! removed in the key, otherwise, will map to false."
  (cond
    ; TODO: LISP not always tighter!
    (.startsWith q "!") (assoc in-map (.substring q 1) true)
    :else (assoc in-map q false)))

(defn read-questions [questions-file]
  "Reads the questions in a question file into a set."
  ; TODO: Try with resources…
  (with-open [rdr (clojure.java.io/reader questions-file)]
    (reduce csv-question-classifier {} (line-seq rdr))))

(defn write-questions [[header & rows] questions-file]
  "Writes the first row of a row-major vector table into a file, one member per row."
  (with-open [wtr (clojure.java.io/writer questions-file)]
    ; TODO: Lazy
    (doall (map #(.write wtr (str % "\n")) header))))


; ----------------------------------------------------------------------------------------------------------------------
; dealing with scoring info construction and file IO

(defn make-q-a-list [header rows]
  "Make a list of question-answer pairs."
  ; TODO: !!! zip
  (flatmap #(map vector header %) rows))

(defn filter-for-scorables [scorables q-a-list]
  "Remove all tuples not corresponding to an entry in scorables from the Q-A-list."
  (filter (fn [[first second]] (contains? scorables first)) q-a-list))

(defn expand-csv-answer [scorables [q a]]
  "Expands a multi-answer (checkbox values as a CSV list) into multiple Q-A pairs."
  (cond
    ; TODO: Coming out of Java array values
    (true? (get scorables q)) (map #(vector q %) (vec (.split a ", ")))
    :else [[q a]]))

(defn expand-csv-answers [scorables q-a-list]
  "Expands all multi-answers in q-a-list."
  (flatmap #(expand-csv-answer scorables %) q-a-list))

(defn make-filter-expand [scorables header rows]
  "Filters out unscorable questions & expands multi-answers."
  (expand-csv-answers scorables (filter-for-scorables scorables (make-q-a-list header rows))))

(defn make-0scorer [scorables]
  "Creates a closure over the scorable answers data for going from input table to Q-A-list"
  ; TODO: !!! Destructuring (1/2)
  (fn [[header & rows]] (reduce
                          (fn [in-assoc path] (assoc-in in-assoc path 0)) {}
                          (make-filter-expand scorables header rows))))

(defn write-score-info [data name scorable-questions]
  "Write scoring info created by make-score-structure in JSON format."
  (with-open [w (clojure.java.io/writer name)]
    ; TODO: !!! Gewöhnungsbedürftiges I/O
    (binding [*out* w]
      (cson/generate-stream ((make-0scorer scorable-questions) data) w {:pretty true}))))


(defn read-score-info [name]
  "Read the scoring info as previously written by write-score-info "
  (with-open [r (clojure.java.io/reader name)]
    (json/read r)))


; ----------------------------------------------------------------------------------------------------------------------
; dealing with main CSV computation & file IO

(defn make-score-r [score-info]
  "Adds the score for one answer for one question to the previous score."
  ; TODO: Higher order (1/3)
  (fn [value [q a]]
    (+ value (get (get score-info q {}) a 0))))


(defn make-participant-scorer [scorables score-info header]
  "Produces a function that scores a participant."
  (fn [row]
    ; TODO: Higher order (2/3)
    (let [q-a-sequence (make-filter-expand scorables header [row])
          scorer (make-score-r score-info)]
      (map str (conj row (reduce scorer 0 q-a-sequence))))))


(defn compute-scored [scorables score-info [header & rows]]
  "Computes each candidate's score."
  ; TODO: Higher order (3/3)
  (let [each-participant (make-participant-scorer scorables score-info header)]
    (cons
      (conj header "Score")
      (map each-participant rows))))


(defn get-csv [filename]
  "Loads the main input data CSV."
  (csv/parse-csv (slurp filename :encoding "UTF-8") :delimiter \;))


(defn write-scores [questions score-info data name]
  "Writes the main (scored) output CSV."
  (with-open [w (clojure.java.io/writer name)]
    (doall (.write w (csv/write-csv (compute-scored questions score-info data) :delimiter \; :quote-char \")))))


; ----------------------------------------------------------------------------------------------------------------------
; UI

(def opts [["-i" "--input=INPUT" "The input CSV." :default "input.csv"]
           ["-q" "--questions=QUESTIONS" "The file with the scorable questions." :default "questions.txt"]
           ["-s" "--scoring=SCORES" "The file with the scored answers." :default "scores.json"]
           ["-o" "--output=OUTPUT" "The output scores." :default "scored.csv"]])

(defn usage [cli-summary]
  "Print usage information."
  (doall (map println
              ["Usage:"
               "  java -jar workshopstats.jar [flags] (questions|scorestruct|scores)"
               ""
               "Flags:"
               cli-summary
               ""])))

; TODO: Creating standalone tools (2/2)
(defn -main [& args]
  "Entrypoint of the script."
  ; TODO: !!! Destructuring (2/2)
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args opts)
        infile (:input options)
        questionsfile (:questions options)
        scoresfile (:scoring options)
        outfile (:output options)
        bad-invocation #(do (usage summary) (System/exit 1))]
    (if (or (not= (count arguments) 1) errors)
      (bad-invocation)
      (case (first arguments)
        "questions" (write-questions (get-csv infile) questionsfile)
        "scorestruct" (write-score-info (get-csv infile) scoresfile (read-questions questionsfile))
        "scores" (write-scores (read-questions questionsfile) (read-score-info scoresfile) (get-csv infile) outfile)
        (bad-invocation)))))
