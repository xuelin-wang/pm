(ns dv.utils
  (:require [clojure.string])
  )

(defn new-window [url content]
  (let [win (.open js/window url "_blank")
        _ (-> win
            (.-document)
            (.open)
            (.write "<html><head/><body></body></html>"))
        ]
    (if (some? content)

      (let [
            init-fn (fn []
                      (let [doc (.-document win)
                            pre (.createElement doc "pre")
                            _ (set! (.-innerText pre) content)
                            body (.-body doc)
                            ]
                          (.appendChild body pre)
                        )
                      )
            ]
        (init-fn)
        ;        (set! (.-onload win) init-fn)
        win
        )
      )
    ;    (.close win)

    )
  )

(defn- to-csv-str [ss0 check-quotes]
  (if (nil? ss0) ""
                (let [ss (str ss0)
                      has-quotes (clojure.string/includes? ss "\"")]
                  (if (and check-quotes (not has-quotes))
                    ss
                    (str "\"" (clojure.string/replace ss "\"" "\"\"") "\"")
                    )
                  )
                )
  )

(defn to-csv [list-of-maps keys header-fn]
  (let [header-row (clojure.string/join "," (map #(to-csv-str % true) (if (nil? header-fn) keys (map header-fn keys))))
        rows
        (map
          (fn [mm]
            (clojure.string/join "," (map #(to-csv-str % true) (map #(get mm %) keys)))
            )
          list-of-maps
          )
        ]
    (str header-row "\n" (clojure.string/join "\n" rows))
    )
  )
