(ns dashboard.middleware
  (:require
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.cors :refer [wrap-cors]]
   [prone.middleware :refer [wrap-exceptions]]
   [ring.middleware.reload :refer [wrap-reload]]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(def middleware
  [#(wrap-defaults % site-defaults)
   wrap-exceptions
   wrap-reload])

(def api-middleware
  [#(wrap-defaults % (-> site-defaults (assoc-in [:security :anti-forgery] false)))
   #(wrap-cors % :access-control-allow-origin [#".*"] 
               :access-control-allow-methods [:get :put :post :delete])
   wrap-exceptions
   wrap-reload])

(def file-upload
  [#(wrap-defaults % (-> site-defaults (assoc-in [:security :anti-forgery] false)))
   wrap-params
   wrap-multipart-params])
