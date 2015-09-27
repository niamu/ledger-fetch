(ns ledger-fetch.core
  (:require [ledger-fetch.banks.chase :as chase]
            [ledger-fetch.banks.tangerine :as tangerine]
            [ledger-fetch.banks.td :as td]))

(defn -main
  []
  (td/login)
  (tangerine/login)
  (chase/get-csv))
