(ns dashboard.druid
  (:require
   [ring.util.http-response :refer [ok]]
   [clj-time.core :as t]
   [clj-http.client :as http-client]
   [clojure.java.io :as io]
   [clojure.data.json :as json]
   [clj-druid.client :as client]))
;  (:import [org.joda.time Period]
;           [org.joda.time.format PeriodFormat]))

;(def default-druid-host (str "http://" (or (System/getenv "DRIUD_HOST") "localhost:8083") "/druid/v2"))
(def default-druid-broker (str "http://" (or (System/getenv "DRUID_BROKER") "localhost:8082") "/druid/v2"))
(def default-druid-router (str "http://" (or (System/getenv "DRUID_ROUTER") "localhost:8888") "/druid/indexer/v1/supervisor"))
;(def druid-client (client/connect {:zk {:host "127.0.0.1:2181" ; can contain multiple hosts separated by commas
;                             :discovery-path "/druid/discovery"
;                                            :node-type "druid:broker"}}))

(prn "broker" default-druid-broker)
(prn "router" default-druid-router)

(def druid-client (client/connect {:hosts [default-druid-broker]}))
(def datasource "finished-games")

;(defn format-millis-difference [^Long millis]
;  (.print (PeriodFormat/getDefault) (Period. (* 1000 60 60 6))))

;(format-millis-difference 25000)

;(format-millis-difference 0)

(def ingestion-spec (io/resource "druid/kafka-spec.json"))

(defn setup [] 
  (http-client/post default-druid-router {:content-type :json 
                                          :body (slurp ingestion-spec)}))

(defn do-query [q]
  (ok (client/query
        druid-client
        client/randomized
        (:queryType q) q)))

(defn round [precision decimal]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* decimal factor)) factor)))

(defn query-builder [& {:keys [game campaigns experiments granularity start-date end-date] :or {granularity :month start-date "1900-01-01T00:00Z" end-date "3000-01-01T00:00Z"}}]
  (let [filter-dimension (fn [dimension value] {:type :selector :dimension dimension :value value})
        campaign-filters (mapv (partial filter-dimension "campaign") campaigns)
        experiment-filters (mapv (partial filter-dimension "experiment") experiments)
        game-filters (when-not (nil? game) [{:type :selector :dimension "game" :value game}])
        all-filters (vec (remove empty? (map (fn [xs] (when-not (empty? xs) {:type :or :fields xs})) [game-filters experiment-filters campaign-filters])))
        q {
           :queryType :timeseries
           ;:queryType :groupBy
           :dataSource datasource
           :granularity granularity
           ;:dimensions ["campaign"]
           :aggregations [
                          {:type :longSum :name "judgement-count" :fieldName "judgements"}
                          {:type :longSum :name "game-count" :fieldName "count"}
                          {:type :longSum :name "total-duration" :fieldName "duration"}
                          {:type :cardinality :name "session-count" :fieldNames ["session_id"]} ; :byRow false}
                          {:type :cardinality :name "player-count" :fieldNames ["player"]}; :byRow false}
                          ]
           :postAggregations [
                              {
                               :type :arithmetic
                               :name "LTJ"
                               :fn "/"
                               :fields [
                                        {:type :finalizingFieldAccess :fieldName "judgement-count"}
                                        {:type :finalizingFieldAccess :fieldName "player-count"}
                                        ]
                               }
                              {
                               :type :arithmetic
                               :name "AjpP"
                               :fn "/"
                               :fields [
                                        {:type :finalizingFieldAccess :fieldName "judgement-count"}
                                        {:type :finalizingFieldAccess :fieldName "session-count"}
                                        ]
                               }
                              {
                               :type :arithmetic
                               :name "ASL"
                               :fn "/"
                               :fields [
                                        {:type :fieldAccess :fieldName "total-duration"}
                                        {:type :finalizingFieldAccess :fieldName "session-count"}
                                        ]
                               }
                              {
                               :type :arithmetic
                               :name "ALP"
                               :fn "/"
                               :fields [
                                        {:type :fieldAccess :fieldName "total-duration"}
                                        {:type :finalizingFieldAccess :fieldName "player-count"}
                                        ]
                               }
                              {
                               :type "arithmetic"
                               :name "throughput"
                               :fn "/"
                               :fields [
                                        {:type :fieldAccess :fieldName "total-duration"}
                                        {:type :finalizingFieldAccess :fieldName "judgement-count"}
                                        ]
                               }
                              ]
           :intervals [ (str start-date "/" end-date) ]
           :context { :grandTotal true
                     :skipEmptyBuckets false
                      :timeout 6000 }
           }]
   (if (empty? all-filters)
     q
     (assoc q :filter { :type :and :fields all-filters }))))

(defn extract-query-results [response]
  (-> response
      (get-in [:body :body])
      (json/read-str)))

(defn query [& params]
  (let [q (apply query-builder params)
        round-decimals (partial round 3)
        round-ints #(Math/round %)
        response (do-query q)
        results (extract-query-results response)
        post-process-record (fn [record]
                              (-> record
                                (update-in ["result" "LTJ"] round-decimals)
                                (update-in ["result" "ALP"] round-decimals)
                                (update-in ["result" "AjpP"] round-decimals)
                                (update-in ["result" "throughput"] round-decimals)
                                ;(update-in ["result" "game-count"] round-ints)
                                (update-in ["result" "player-count"] round-ints)
                                (update-in ["result" "session-count"] round-ints)))]
    (map post-process-record results)))

;(doseq [result (extract-query-results (do-query (query-builder :game "tileattack.com")))]
;  (prn result))

;(query :game "wordclicker.com" :granularity :month)


(defn dimension
  ([dimension-name] (dimension dimension-name nil))
  ([dimension-name game]
   (let [q (merge
             {
              :queryType :topN
              :dataSource datasource
              :dimension dimension-name
              :threshold 1000
              :metric "judgements"
              :granularity :all
              :aggregations [{
                              :type :longSum
                              :name "judgements"
                              :fieldName "count"
                              }]
              :context {:timeout 6000}
              :intervals [ "1900-08-31T00:00:00.000/3000-09-03T00:00:00.000" ]
              }
             (when (some? game) {:filter {:type :selector :dimension "game" :value game}}))
         response (do-query q)]
     (-> response
         (get-in [:body :body])
         (json/read-str)
         (first)
         (get "result")))))

(defn dimensions
  ([] (dimensions nil))
  ([game]
  (let [dimension-names ["experiment" "game" "campaign"]
        results (pmap (fn [dimension-name]
                        (let [result (if (or (nil? game) (= dimension-name "game"))
                                       (dimension dimension-name)
                                       (dimension dimension-name game))
                              processed-result (mapv #(get % dimension-name) result)]
                          {dimension-name processed-result}))
                      dimension-names)]
    (apply merge results))))

;(dimension "campaign")

;(dimensions)
