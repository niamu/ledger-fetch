(ns ledger-fetch.banks.td
  (:require [clj-http.client :as client]
            [clojure.string :as string]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [ledger-fetch.config :refer [config]]))

(def cs (clj-http.cookies/cookie-store))

(def baseurl (str "https://easyweb.td.com/waw/"))

(defn send-credentials
  []
  (client/post (str baseurl "idp/authenticate.htm")
               {:form-params {:login:AccessCard (-> config :banks :td :username)
                              :login:Webpassword (-> config :banks :td :password)}
                :cookie-store cs}))

(defn get-question
  [authurl]
  (let [html (:body (client/get authurl
                                {:cookie-store cs}))
        element (s/select
                 (s/id "MFAChallengeForm:question")
                 (hickory/as-hickory
                  (hickory/parse html)))]
    (string/trim (first (:content (first element))))))

(defn get-answer
  [question]
  (:answer
   (first (filter
           (fn
             [challenge]
             (= question
                (:question challenge)))
           (-> config :banks :td :challenge)))))

(defn challenge-response
  [authurl]
  (let [question (get-question authurl)
        answer (get-answer question)]
    (client/post authurl
                 {:form-params {:MFAChallengeForm "MFAChallengeForm"
                                :MFAChallengeForm:answer answer
                                :MFAChallengeForm:next "Next"
                                :javax.faces.ViewState "e2s1"}
                  :cookie-store cs
                  :force-redirects true})))

(defn parse-epoch-form1
  [html]
  (let [form (first
              (s/select
               (s/tag :form)
               (hickory/as-hickory
                (hickory/parse html))))
        inputs (mapv :attrs
                     (s/select
                      (s/child
                       (s/tag :form)
                       (s/tag :input))
                      (hickory/as-hickory
                       (hickory/parse html))))]
    (:body
     (client/post (str baseurl "ezw/" (:action (:attrs form)))
                  {:form-params {:ConnectID (:value (first inputs))
                                 :TIME (:value (second inputs))
                                 :SKIP_DME "null"
                                 :channelID ""}
                   :cookie-store cs}))))

(defn parse-epoch-form2
  [html]
  (let [form (first
              (s/select
               (s/tag :form)
               (hickory/as-hickory
                (hickory/parse html))))
        input (first
               (mapv :attrs
                     (s/select
                      (s/child
                       (s/tag :form)
                       (s/tag :input))
                      (hickory/as-hickory
                       (hickory/parse html)))))]
    (:body (client/post (str baseurl "ezw/" (:action (:attrs form)))
                  {:form-params {:TIME (:value input)}
                   :cookie-store cs}))))

(defn get-summary-url
  [html]
  (let [frame (first
               (s/select
                (s/tag :frame)
                (hickory/as-hickory
                 (hickory/parse html))))]
    (:src (:attrs frame))))

(defn login-redirect
  [authurl]
  (client/get authurl
              {:cookie-store cs})
  (client/post (str baseurl "ezw/CIPLoginRedirect?LN=en_CA")
               {:form-params {:LN "en_CA"
                              :L4User "true"
                              :mode "resume"}
                :cookie-store cs
                :force-redirects true})
  (client/post (str baseurl "ezw/redirectToLogin.jsp")
               {:form-params {:parameterList "?MODE=resume&L4USER=true&LN=en_CA"
                              :servletName "servlet/ca.tdbank.banking.servlet.LoginServlet"}
                :cookie-store cs
                :force-redirects true})
  (get-summary-url
   (parse-epoch-form2
    (parse-epoch-form1
     (:body
      (client/get (str baseurl "ezw/servlet/ca.tdbank.banking.servlet.LoginServlet")
                  {:query-params {:MODE "resume"
                                  :L4USER "true"
                                  :LN "en_CA"}
                   :cookie-store cs}))))))

(defn get-mortgage-balance
  [summary-url]
  (let [html (:body (client/get summary-url
                                {:cookie-store cs}))
        balance (first
                 (s/select
                  (s/descendant
                   (s/class "td-target-creditcards")
                   (s/tag :td))
                  (hickory/as-hickory
                   (hickory/parse html))))]
    (println (string/trim (first (:content balance))))))

(defn login
  []
  (client/get (str baseurl "idp/login.htm")
              {:cookie-store cs})
  (let [authurl (:location (:headers (send-credentials)))]
    (if (not (= authurl
                "https://easyweb.td.com/waw/idp/goto.htm"))
      (challenge-response authurl)
      (get-mortgage-balance
       (login-redirect authurl)))))
