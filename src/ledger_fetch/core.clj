(ns ledger-fetch.core
  (:require [ledger-fetch.banks.chase :as chase]
            [ledger-fetch.banks.tangerine :as tangerine]))

(defn -main
  []
  (tangerine/login)
  (chase/get-csv))
