(ns dashboard.upload
  (:require
    [dashboard.kafka :as kafka]
    [clojure.java.io :as io]
    [clojure.data.csv :as csv]
    [clojure.spec.alpha :as s]))

(defn csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data)
            (map keyword)
            repeat)
       (rest csv-data)))

(def ^:dynamic *max-in-session-gap* (* 20 1000 60))

(s/def ::start integer?)
(s/def ::duration integer?)
(s/def ::judgements int?)
(s/def ::player string?)
(s/def ::experiment (s/nilable string?))
(s/def ::campaign (s/nilable string?))

(s/def ::game-record (s/keys :req-un [::start ::duration ::judgements ::player]
                            :opt-un [::experiment ::campaign]))

(defn batch-process [game file]
  (let [parseInt #(try (Integer/parseInt %) (catch NumberFormatException e nil))
        parseLong #(try (Long/parseLong %) (catch NumberFormatException e nil))
        create-uuid #(.toString (java.util.UUID/randomUUID))
        no-interaction-time-limit (* 1000 60 60 6)
        no-interaction-game (fn [{:keys [duration judgements]}] (and (zero? judgements) (> duration no-interaction-time-limit)))
        bot-game (fn [{:keys [duration judgements]}] (and (zero? judgements) (< duration 500)))
        update-types (fn [record] (-> record 
                                      (update :start parseLong)
                                      (update :duration parseLong)
                                      (update :judgements parseInt)))
        add-game #(assoc % :game game)]
  (with-open [reader (io/reader file)]
    (loop [rows (doall (->> reader 
                     csv/read-csv 
                     csv-data->maps 
                     (map update-types)
                     (map add-game)
                     (filter #(s/valid? ::game-record %))
                     (remove no-interaction-game)
                     (remove bot-game)))
            last-game {}]
      (when-not (empty? rows)
        (let [{:keys [start duration player] :as current} (first rows)
              last-game-end (get-in last-game [player :game-end] 0)
              in-session (+ last-game-end *max-in-session-gap*)
              game-end (+ start duration)
              session-id (if (> in-session start) 
                           (get-in last-game [player :session-id]) 
                           (create-uuid))]
          (kafka/send-game (assoc current :session_id session-id))
          (recur (rest rows) (assoc last-game player {:session-id session-id :game-end game-end}))))))))

;(with-open [reader (io/reader "ta.csv")]
;  (let [parseInt #(try (Integer/parseInt %) (catch NumberFormatException e nil))
;        parseLong #(try (Long/parseLong %) (catch NumberFormatException e nil))
;        add-game #(assoc % :game "tileattack.com")
;        update-types (fn [record] (-> record 
;                                      (update :start parseLong)
;                                      (update :duration parseLong)
;                                      (update :judgements parseInt)))
;        rows (doall (->> reader 
;                   csv/read-csv 
;                   csv-data->maps 
;                   (map update-types)
;                   (map add-game)
;                   (filter #(s/valid? ::game-record %))))]
;    (count rows)))
;(batch-process "tileattack.com" "test.csv")
