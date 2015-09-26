(ns ledger-fetch.banks.chase
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [ledger-fetch.config :refer [config]]))

(def cs (clj-http.cookies/cookie-store))

(def baseurl (str "https://online.chasecanada.ca/ChaseCanada_Consumer/"))

(defn get-answer
  []
  ; There's only one possible question/answer with Chase
  (first
   (mapv :answer
         (-> config :banks :chase :challenge))))

(defn challenge-response
  []
  (client/post (str baseurl "SecondaryAuth.do")
               {:form-params {:hintanswer (get-answer)}
                :cookie-store cs}))

(defn login
  []
  (client/post (str baseurl "ProcessLogin.do")
               {:form-params {:username (-> config :banks :chase :username)
                              :password (-> config :banks :chase :password)}
                :cookie-store cs})
  (challenge-response)
  (client/get (str baseurl "TransHistory.do")
              {:cookie-store cs}))

(defn get-csv
  []
  (login)
  (println
   (:body
    (client/post (str baseurl "DownLoadTransaction.do")
                 {:form-params {:downloadType "csv"
                                :cycleDate "00"}
                  :cookie-store cs}))))
