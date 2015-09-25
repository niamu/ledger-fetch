(ns ledger-fetch.banks.tangerine
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [ledger-fetch.config :refer [config]]))

(defn challenge-response
  [html bank]
  (first (mapv :answer
               (filter
                (fn
                  [challenge]
                  (= (first (:content html))
                     (:question challenge)))
                (-> config :banks bank :challenge)))))

(defn login
  []
  (let [baseurl (str "https://secure.tangerine.ca/web/")
        my-cs (clj-http.cookies/cookie-store)]
    (client/get (str baseurl "InitialTangerine.html")
                {:query-params {:command "displayLogin"
                                :device "web"
                                :locale "en_CA"}
                 :cookie-store my-cs})
    (client/post (str baseurl "Tangerine.html")
                 {:form-params {:ACN (-> config :banks :tangerine :username)
                                :command "PersonalCIF"}
                  :cookie-store my-cs})
    (client/post (str baseurl "Tangerine.html")
                 {:form-params {:Answer (challenge-response
                                         (first (s/select
                                                 (s/child (s/class "content-main-wrapper")
                                                          (s/tag :p))
                                                 (hickory/as-hickory
                                                  (hickory/parse
                                                   (:body
                                                    (client/get (str baseurl "Tangerine.html")
                                                                {:query-params {:command "displayChallengeQuestion"}
                                                                 :cookie-store my-cs}))))))
                                         :tangerine)
                                :command "verifyChallengeQuestion"}
                  :cookie-store my-cs})
    (client/post (str baseurl "Tangerine.html")
                 {:form-params {:PIN (-> config :banks :tangerine :password)
                                :command "validatePINCommand"}
                  :cookie-store my-cs})
    (client/get (str baseurl "Tangerine.html")
                {:query-params {:command "PINPADPersonal"}
                 :cookie-store my-cs})
    (println (:body (client/get (str baseurl "Tangerine.html")
                                {:query-params {:command "displayAccountSummary"
                                                :fill 1}
                                 :cookie-store my-cs})))))
