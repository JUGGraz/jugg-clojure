(ns com.xaidat.workshopstats.tests
  (:require [com.xaidat.workshopstats :as dut]
            [clojure.test :refer :all]))

; TODO: Tests

(deftest questions-classifier-test
  (is (= {"q" false}
         (dut/csv-question-classifier {} "q")))
  (is (= {"q" true}
         (dut/csv-question-classifier {} "!q")))
  (is (= {"q" false "x" true}
         (dut/csv-question-classifier {"x" true} "q"))))

(deftest make-qa-list
  (is (= [["q1" "q1-1"] ["q2" "q2-1"] ["q3" "q3-1-1,q3-1-2"] ["q1" "q1-2"] ["q2" "q2-2"] ["q3" "q3-2-1,q3-2-2,q3-2-3"]]
         (let [[header & rows]
               [["q1" "q2" "q3"] ["q1-1" "q2-1" "q3-1-1,q3-1-2"] ["q1-2" "q2-2" "q3-2-1,q3-2-2,q3-2-3"]]]
           (dut/make-q-a-list header rows)))))

(deftest filtering-scorables
  (is (= [["q1" "q1-1"] ["q3" "q3-1-1,q3-1-2"] ["q1" "q1-2"] ["q3" "q3-2-1,q3-2-2,q3-2-3"]]
         (dut/filter-for-scorables {"q1" false "q3" true}
                                   [["q1" "q1-1"] ["q2" "q2-1"] ["q3" "q3-1-1,q3-1-2"]
                                    ["q1" "q1-2"] ["q2" "q2-2"] ["q3" "q3-2-1,q3-2-2,q3-2-3"]]))))
