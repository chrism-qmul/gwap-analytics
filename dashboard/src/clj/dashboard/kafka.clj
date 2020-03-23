(ns dashboard.kafka
  (:require
   [clojure.set :refer [rename-keys]]
   [jackdaw.streams :as j]
   [jackdaw.admin :as admin]
   [clojure.spec.alpha :as s]
   [jackdaw.client :refer [producer produce!]]
   [jackdaw.serdes :refer [string-serde]]
   [jackdaw.serdes.json :refer [serde]])) 

(defn check-spec [spec data]
  (if (s/valid? spec data)
    true
    (let [explanation (s/explain-str spec data)]
      (throw (IllegalArgumentException. explanation)))))

(def streams-config 
  {"application.id" "gwap-analytics"
   "bootstrap.servers" (or (System/getenv "KAKFA_BOOTSTRAP_SERVERS") "kafka:9092")
   "zookeeper.connect" (or (System/getenv "ZOOKEEPER_SERVERS") "localhost:2181")
   "cache.max.bytes.buffering" "0"})

(def game-events-input-topic 
  {:topic-name "game-events-input"
   :partition-count 1
   :replication-factor 1
   :key-serde (string-serde)
   :value-serde (serde)})

(def finished-games-topic 
  {:topic-name "finished-games"
   :partition-count 1
   :replication-factor 1
   :key-serde (string-serde)
   :value-serde (serde)})

(def game-updates-topic
  {:topic-name "game-updates"
   :partition-count 1
   :replication-factor 1
   :key-serde (string-serde)
   :value-serde (serde)})

(def tproducer (delay (producer streams-config game-events-input-topic)))

(def admin-client (delay (admin/->AdminClient streams-config)))

(defn create-topics! []
  (let [all-topics [game-events-input-topic finished-games-topic game-updates-topic]
        topics-to-create (remove (partial admin/topic-exists? @admin-client) all-topics)]
    (when-not (empty? topics-to-create)
      (admin/create-topics! @admin-client topics-to-create))))

;(admin/list-topics @admin-client)

(defn setup []
  (create-topics!))

;(setup)

;(let [streams-builder (j/streams-builder)
  ;    stream-in (j/kstream streams-builder game-events-input-topic)
  ;    stream-out (j/kstream streams-builder finished-games-topic)]
  ;(-> stream-in
      ;(j/aggregate (fn [] {}) (fn ))))
  

(s/def ::start integer?)
(s/def ::type #{"start" "judgement" "end"})
(s/def ::duration integer?)
(s/def ::session_id string?)
(s/def ::judgements int?)
(s/def ::player string?)
(s/def ::game string?)
(s/def ::experiment (s/nilable string?))
(s/def ::campaign (s/nilable string?))

(s/def ::game-event-record (s/keys :req-un [::game ::type ::player]
                            :opt-un [::experiment ::campaign]))

(defn send-game-event [{:keys [player game] :as params}]
  {:pre [(check-spec ::game-event-record params)]}
  (let [game-event (merge {:timestamp (System/currentTimeMillis)} params)
        k (str player ":" game)]
    (produce! @tproducer game-events-input-topic k game-event)))

(s/def ::game-record (s/keys :req-un [::game ::start ::duration ::judgements ::player ::session_id]
                            :opt-un [::experiment ::campaign]))

(defn send-game [{:keys [player game] :as params}]
  {:pre [(s/valid? ::game-record params)]}
  (let [game-event (rename-keys params {:start :timestamp})
        k (str player ":" game)]
    (produce! @tproducer finished-games-topic k game-event)))

;(send-game-event {:type "start" :game "tileattack.com" :experiment "exp1" :campaign "campaign1" :player "p1"})
;(send-game {:start 123123123 :game "tileattack.com" :duration 1000 :judgements 10 :experiment "exp1" :session_id "test-session" :campaign "campaign1" :player "p1"})

;(def builder (j/streams-builder))
;(j/kstream builder game-events-input-topic)
