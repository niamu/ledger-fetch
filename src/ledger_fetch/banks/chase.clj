(ns ledger-fetch.banks.chase
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [ledger-fetch.config :refer [config]]))

(defn download
  []
  (let [baseurl (str "https://online.chasecanada.ca/ChaseCanada_Consumer/")
        my-cs (clj-http.cookies/cookie-store)]
    (client/post (str baseurl "ProcessLogin.do")
                 {:form-params {:username (-> config :banks :chase :username)
                                :password (-> config :banks :chase :password)}
                  :cookie-store my-cs})
    (client/post (str baseurl "SecondaryAuth.do")
                 {:form-params {:hintanswer (first
                                             (mapv :answer
                                                   (-> config :banks :chase :challenge)))}
                  :cookie-store my-cs})
    (client/get (str baseurl "TransHistory.do")
                {:cookie-store my-cs})
    (println (:body (client/post (str baseurl "DownLoadTransaction.do")
                                 {:form-params {:downloadType "csv"
                                                :cycleDate "00"}
                                  :cookie-store my-cs})))))
