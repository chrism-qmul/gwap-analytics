(ns dashboard.middleware
  (:require
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(def middleware
  [#(wrap-defaults % site-defaults)])

(def api-middleware
  [#(wrap-defaults % (-> site-defaults (assoc-in [:security :anti-forgery] false)))
   #(wrap-cors % :access-control-allow-origin [#".*"] 
               :access-control-allow-methods [:get :put :post :delete])])

(def file-upload
  [#(wrap-defaults % (-> site-defaults (assoc-in [:security :anti-forgery] false)))
   wrap-params
   wrap-multipart-params])
