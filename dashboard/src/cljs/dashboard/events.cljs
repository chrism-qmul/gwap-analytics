(ns dashboard.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
      [dashboard.db :as db]
      [cljs.core.async :refer [<!]]
      [cljs-http.client :as http]
      [clojure.string :as s]
      [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch ->interceptor]]))

(reg-event-db
  :initialize 
  (fn [app-state _]
    (if (empty? app-state)
      db/initial
      app-state)))

;(defn update-results []
;  )
;
;(reg-event-fx :fetch-results
;              (fn [{:keys [db]} _]
;                (let [save-db (apply dissoc (cons db do-not-save))]
;                  {:dispatch [:add-notification [:span [:span.glyphicon.glyphicon-floppy-open] " Saving game..." ]]
;                                                   :socket-send {:channel [:game/save-session save-db]}})))

;(reg-event-fx :update-results
;              (fn [params] 
;                (prn "update-results" params)
;                {:dispatch-n [[:loading true]]
;                 :fetch-results params}))

(defn db-event-handle [after]
  (fn [{:keys [db]} event]
    {:db (apply after (cons db (rest event)))}))

(reg-event-fx :result-refresh
              (fn [_ _]
                {:dispatch [:update-results]
                 :dispatch-later [{:ms 5000 :dispatch [:result-refresh]}]}))

(reg-event-fx :start-loading
              (fn [{:keys [db]} _]
                {:db (update db :loading inc)}))

(reg-event-fx :set-granularity
              (fn [{:keys [db]} [_ granularity]]
                {:db (assoc db :granularity granularity)
                 :dispatch [:update-results]}))

(reg-event-fx :set-game
              (fn [{:keys [db]} [_ game]]
                {:db (update db :filters merge {:game game :campaign #{} :experiment #{}})
                 :dispatch [:update-results]}))

(reg-event-fx :set-start-time
              (fn [{:keys [db]} [_ date]]
                {:db (assoc-in db [:filters :start-time] date)
                 :dispatch [:update-results]}))

(reg-event-fx :set-end-time
              (fn [{:keys [db]} [_ date]]
                {:db (assoc-in db [:filters :end-time] date)
                 :dispatch [:update-results]}))

(reg-event-fx :slice-ordinal-dimension
              (fn [{:keys [db]} [_ dimension-name value status]]
                (let [add-or-remove (if status conj disj)]
                  {:db (update-in db [:filters dimension-name] add-or-remove value)
                   :dispatch [:update-results]})))

(reg-event-fx :done-loading
              (fn [{:keys [db]} _]
                {:db (update db :loading dec)}))

(reg-event-fx :update-results-db 
              (fn [{:keys [db]} [_ results]]
                {:db (assoc db :results results)
                 :dispatch [:done-loading]}))

(reg-event-fx :update-dimensions-db 
              (fn [{:keys [db]} [_ results]]
                {:db (assoc db :dimensions results)
                 :dispatch [:done-loading]}))

(defn query-body-from-db [{:keys [granularity filters] :as db}]
  (let [str-join (fn [xs] (s/join "," xs))
        query-filters (-> {}
            (assoc :game (filters :game))
            (assoc :start-date (filters :start-time))
            (assoc :end-date (filters :end-time))
            (assoc :campaigns (str-join (filters :campaign)))
            (assoc :experiments (str-join (filters :experiment)))
            (assoc :granularity (name granularity)))]
    query-filters))

(reg-event-fx :update-results
              (fn [{:keys [db]}] 
                (let [params (query-body-from-db db)]
                  {:dispatch [:start-loading]
                   :fetch-results params
                   :fetch-dimensions params})))

(reg-event-fx :update-dimensions
              (fn [{:keys [event]}] 
                (let [[_ params] event]
                  {:dispatch [:start-loading]
                   :fetch-dimensions params})))

(reg-fx :fetch-results
        (fn [params]
          (go
            (let [{:keys [body] :as response} (<! (http/get "/query" {:query-params params}))]
              (dispatch [:update-results-db body])))))

(reg-fx :fetch-dimensions
        (fn [params]
          (go
            (let [{:keys [body] :as response} (<! (http/get "/dimensions" {:query-params params}))]
              (dispatch [:update-dimensions-db body])))))

;(reg-event-fx :update-results-db (db-event-handle update-results-db))
