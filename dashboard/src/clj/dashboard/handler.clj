(ns dashboard.handler
  (:require
   [dashboard.upload :as upload]
   [reitit.ring :as reitit-ring]
   [dashboard.middleware :refer [middleware api-middleware] :as middleware]
   [clojure.string :as s]
   [clojure.walk :refer [keywordize-keys]]
   [medley.core :as m]
   [digest :as digest]
   [dashboard.druid :as druid]
   [dashboard.kafka :as kafka]
   [clojure.data.json :as json]
   [hiccup.page :refer [include-js include-css html5]]
   [config.core :refer [env]]))

(def mount-target
  [:div#app
   [:h2 "Welcome to dashboard"]
   [:p "please wait while Figwheel is waking up ..."]
   [:p "(Check the js console for hints if nothing exciting happens.)"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")
   (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css")
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
   (head)
   [:body {:class "body-container"}
    mount-target
    (include-js "/js/app.js")]))

(defn index-handler
  [_request]
  ;(prn _request)
  {:status 200
   :headers {"Content-Type" "text/html"}
   :session {:store true}
   :body (loading-page)})

(defn dimension-list-handler [{:keys [params]}] 
  (let [{:keys [game]} params]
    {:status 200 
     :headers {"Content-Type" "application/json"} 
     :body (json/write-str (druid/dimensions game))}))

(defn query-handler
  [{:keys [params]}]
  ;(prn "before:" params (type params) (m/remove-vals empty? params))
  (let [params (-> params
                  (#(m/remove-vals empty? %))
                  (m/update-existing :campaigns #(s/split % #","))
                  (m/update-existing :experiments #(s/split % #","))
                  (vec)
                  (#(mapcat identity %)))
                  ]
   ; (prn "after: " params)
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str 
             (apply druid/query params)
             )}))

(defn send-analytics-event [{game :host event-type :type :keys [experiment campaign player]}]
  (kafka/send-game-event {:type event-type 
                          :game game 
                          :player player
                          :experiment experiment 
                          :campaign campaign}))

(def api-success-response {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str {:success true})})

(def api-error-response {:status 500
     :headers {"Content-Type" "application/json"}
     :body (json/write-str {:success false})})

(defmulti analytics-handler :request-method)

(defmethod analytics-handler :post [{:keys [form-params multipart-params] :as req}]
  ;(prn "form-params" form-params)
  ;(prn "mutlipart-params" multipart-params)
  ;(prn "the params" (first (remove empty? [form-params multipart-params])))
  (let [params (first (remove empty? [form-params multipart-params]))
        event-send (send-analytics-event (keywordize-keys params))]
    api-success-response))

(defmethod analytics-handler :get [{:keys [params]}]
  (send-analytics-event params)
  api-success-response)
;
;(defn analytics-handler [{:keys [params] :as req}]
;  (prn req))
;  (let [{event-type type :keys [game experiment campaign player]} params]
;    (kafka/send-game-event {:type event-type 
;                            :game game 
;                            :player player
;                            :experiment experiment 
;                            :campaign campaign})
;    {:status 200
;     :headers {"Content-Type" "application/json"}
;     :body (json/write-str {:success true})}))

(defn upload-handler [{:keys [params]}] 
  (let [file (get-in params [:file :tempfile])
        {:keys [game securitykey]} params]
    (if (or (nil? file) (nil? game))
      api-error-response
      (do
        (upload/batch-process game file)
        api-success-response))))

(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/" {:get {:handler index-handler :middleware middleware/middleware}}]
     ["/query" {:get {:handler query-handler} :middleware middleware/api-middleware}]
     ["/dimensions" {:get {:handler dimension-list-handler :middleware middleware/api-middleware}}]
     ["/upload" {:post {:handler upload-handler :middleware middleware/file-upload}}]
     ["/analytics" {:get {:handler analytics-handler :middleware middleware/api-middleware}
                    :post {:handler analytics-handler :middleware middleware/api-middleware}}]
     ["/items"
      ["" {:get {:handler index-handler}}]
      ["/:item-id" {:get {:handler index-handler
                          :parameters {:path {:item-id int?}}}}]]
     ["/about" {:get {:handler index-handler}}]])
   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))
   ))
   ;{:middleware api-middleware}))
