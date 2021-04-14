(ns simulator40k.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [simulator40k.core-test]))

(doo-tests 'simulator40k.core-test)

