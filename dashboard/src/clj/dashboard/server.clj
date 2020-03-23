(ns dashboard.server
    (:require
     [dashboard.handler :refer [app]]
     [dashboard.kafka :as kafka]
     [config.core :refer [env]]
     [ring.adapter.jetty :refer [run-jetty]])
    (:gen-class))

(defn -main [& args]
  (let [port (or (env :port) 3000)]
    (kafka/setup)
    (run-jetty app {:port port :join? false})))
