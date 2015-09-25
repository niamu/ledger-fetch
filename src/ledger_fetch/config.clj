(ns ledger-fetch.config
  (:require [clojure.edn :as edn]))

(def config
  (edn/read-string (slurp "config.edn")))
