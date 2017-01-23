(ns dv.crypt-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [dv.crypt]))

(deftest test-aes
  (map
    (fn [key]
      (let [aes (dv.crypt/new-aes key)]
        (map (fn [ss]
               (let [cypher-text (dv.crypt/aes-encrypt aes ss)
                     text (dv.crypt/aes-decrypt aes cypher-text)]
                 (is (= ss text)))
               )
             ["", "aaaaa", "bbbbb", "abcdefabcdef", "123ersdfcvsdfsdf f w fwf"])
        )
      ["ab12cde456", "124", "bsfwfwfcq", "3243565474746758"])
    )
  )

(map
  (fn [key]
    (let [aes (dv.crypt/new-aes key)]
      (map (fn [ss]
             (let [cypher-text (dv.crypt/aes-encrypt aes ss)
                   text (dv.crypt/aes-decrypt aes cypher-text)]
               (println (str "=?" (= ss text) " ss:" ss)))
             )
           ["", "aaaaa", "bbbbb", "abcdefabcdef", "123ersdfcvsdfsdf f w fwf"])
      )
    ["ab12cde456", "124", "bsfwfwfcq", "3243565474746758"])
  )