(ns ledger-fetch.banks.tangerine
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [ledger-fetch.config :refer [config]]))

(def cs (clj-http.cookies/cookie-store))

(def baseurl (str "https://secure.tangerine.ca/web/"))

(defn send-username
  []
  (client/post (str baseurl "Tangerine.html")
               {:form-params {:ACN (-> config :banks :tangerine :username)
                              :command "PersonalCIF"}
                :cookie-store cs}))

(defn send-password
  []
  (client/post (str baseurl "Tangerine.html")
               {:form-params {:PIN (-> config :banks :tangerine :password)
                              :command "validatePINCommand"}
                :cookie-store cs}))

(defn get-answer
  []
  (let [html (:body
              (client/get (str baseurl "Tangerine.html")
                          {:query-params {:command "displayChallengeQuestion"}
                           :cookie-store cs}))
        element (s/select
                 (s/child (s/class "content-main-wrapper")
                          (s/tag :p))
                 (hickory/as-hickory
                  (hickory/parse html)))
        question (first (:content (first element)))]
    (first (mapv :answer
                 (filter
                  (fn
                    [challenge]
                    (= question
                       (:question challenge)))
                  (-> config :banks :tangerine :challenge))))))

(defn challenge-response
  []
  (client/post (str baseurl "Tangerine.html")
               {:form-params {:Answer (get-answer)
                              :command "verifyChallengeQuestion"}
                :cookie-store cs}))

(defn login
  []
  ; Initial login page request to setup cookies
  (client/get (str baseurl "InitialTangerine.html")
              {:query-params {:command "displayLogin"
                              :device "web"
                              :locale "en_CA"}
               :cookie-store cs})
  (send-username)
  (challenge-response)
  (send-password)
  (client/get (str baseurl "Tangerine.html")
              {:query-params {:command "PINPADPersonal"}
               :cookie-store cs})
  (println
   (:body
    (client/get (str baseurl "Tangerine.html")
                {:query-params {:command "displayAccountSummary"
                                :fill 1}
                 :cookie-store cs}))))
