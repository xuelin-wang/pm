(ns dv.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [dv.core-test]))

(doo-tests 'dv.core-test)

