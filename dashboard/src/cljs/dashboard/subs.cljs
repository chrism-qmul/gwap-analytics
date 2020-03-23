(ns dashboard.subs 
    (:require 
      [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub :loading
         (fn [db _] (pos? (:loading db))))

(reg-sub :results
         (fn [db _] (:results db)))

(reg-sub :dimensions
         (fn [db _] (:dimensions db)))

(reg-sub :dimension
         :<- [:dimensions]
         (fn [dimensions [_ dimension-name]]
           (get dimensions dimension-name)))

(reg-sub :filters
         (fn [db _] (:filters db)))

(reg-sub :filter
         :<- [:filters]
         (fn [filters [_ filter-name]]
           (get filters filter-name)))

(reg-sub :granularity
         (fn [db _] (:granularity db)))

(reg-sub :final-results
         :<- [:results]
         (fn [results _] (some-> (filter #(-> % :timestamp nil?) results) first :result)))

(reg-sub :final-result
         :<- [:final-results]
         (fn [results [_ result-name]] (get results result-name)))
