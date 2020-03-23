(ns dashboard.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [dashboard.bootstrap :as bs]
   [reagent.session :as session]
   [re-frame.core :refer [subscribe dispatch]]
   [dashboard.subs]
   [dashboard.events]
   [reitit.frontend :as reitit]
   [cljs-http.client :as http]
   [cljsjs.recharts]
   [goog.i18n.NumberFormat.Format]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [clerk.core :as clerk]
   [cljs-time.format :as datetime-format]
   [accountant.core :as accountant]
   [goog.date.duration :as duration]
   [cljs.core.async :refer [<!]]))

(enable-console-print!)

(def composed-chart (reagent/adapt-react-class (aget js/Recharts "ComposedChart")))
(def responsive-container (reagent/adapt-react-class (aget js/Recharts "ResponsiveContainer")))
(def brush (reagent/adapt-react-class (aget js/Recharts "Brush")))
(def area-chart (reagent/adapt-react-class (aget js/Recharts "AreaChart")))
(def line-chart (reagent/adapt-react-class (aget js/Recharts "LineChart")))
(def tooltip (reagent/adapt-react-class (aget js/Recharts "Tooltip")))
(def line (reagent/adapt-react-class (aget js/Recharts "Line")))
(def area (reagent/adapt-react-class (aget js/Recharts "Area")))
(def bar (reagent/adapt-react-class (aget js/Recharts "Bar")))
(def cartesian-grid (reagent/adapt-react-class (aget js/Recharts "CartesianGrid")))
(def x-axis (reagent/adapt-react-class (aget js/Recharts "XAxis")))
(def y-axis (reagent/adapt-react-class (aget js/Recharts "YAxis")))

(def decimal-formatter 
  (-> (goog.i18n.NumberFormat. goog.i18n.NumberFormat.Format.DECIMAL)
      (.setMinimumFractionDigits 2)
      (.setMaximumFractionDigits 2)
      (.setShowTrailingZeros false)))

(def int-formatter 
  (-> (goog.i18n.NumberFormat. goog.i18n.NumberFormat.Format.DECIMAL)
      (.setMinimumFractionDigits 0)
      (.setMaximumFractionDigits 0)
      (.setShowTrailingZeros false)))


;; -------------------------
;; Routes
;(go (.log js/console (<! (http/get "/query" {:query-params {}}))))

;(defn update-dimension [dimension]
;  (go 
;    (let [response (<! (http/get (str "/dimension/" dimension)))]
;      (swap! db assoc-in [:dimensions dimension] (:body response)))))

;(defn date-formatter [utc-time-string]
;  ())


(def router
  (reitit/router
   [["/" :index]
    ["/items"
     ["" :items]
     ["/:item-id" :item]]
    ["/about" :about]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

(path-for :about)
;; -------------------------
;; Page components

(def time-formatters 
  {:day (datetime-format/formatter "d/MM")
   :month (datetime-format/formatter "MMM YYYY")
   :year (datetime-format/formatter "YYYY")})

(def xaxis-time-renderer 
  (memoize (fn [granularity timestamp]
             (let [formatter (get time-formatters granularity)]
               (case granularity
                 :day (str (subs timestamp 8 10) "/" (subs timestamp 5 7))
                 :month (datetime-format/unparse formatter (datetime-format/parse timestamp)) 
                 :year (datetime-format/unparse formatter (datetime-format/parse timestamp))
                 (str "ERROR:" timestamp))))))

(def formatters {:int #(.format int-formatter %)
                    :duration #(duration/format %)
                    :dec #(.format decimal-formatter %)})

(defn stats-chart [metric &{:keys [formatting] :or {formatting :dec}}]
  (let [data @(subscribe [:results])
        granularity @(subscribe [:granularity])
        this-xaxis-time-renderer (partial xaxis-time-renderer granularity)
        not-total (remove #(-> % :timestamp nil?) data)
        xticks (mapv :timestamp not-total)
        xtick-formatter (formatters formatting)
        datapoints (mapv #(-> % :result metric) not-total)]
    ;[:p (str xticks)]
    ;[:p (str datapoints)]
    [responsive-container {:height 300 :width "100%"}
     [composed-chart {:syncId :graphs :data (for [dp data
                              :let [timestamp (-> dp :timestamp)
                                    measurement (get-in dp [:result metric])]
                              :when (some? timestamp)]
                          {:name (this-xaxis-time-renderer timestamp) :measurement measurement})}
      [:defs
       [:linearGradient#bluecolorgrad {:x1 0 :y1 0 :x2 0 :y2 1}
        [:stop {:offset "5%" :stop-color "#8884d8" :stop-opacity 0.8}]
        [:stop {:offset "95%" :stop-color "#8884d8" :stop-opacity 0}]]]
      ;[line {:type :monotone :dataKey :measurement :stroke "#8884d8"}]
      [cartesian-grid {:stroke "#ccc" :strokeDashArray "3 3"}]
      [x-axis {:dataKey :name}]
      [y-axis {:tickFormatter xtick-formatter}]
      [tooltip]
      [area {:dataKey :measurement :stroke "#8884d8" :fillOpacity 1 :fill "url(#bluecolorgrad)"}]
      ;[bar {:type :monotone :dataKey :measurement :stroke "#8884d8" :fillOpacity 1 :fill "url(#bluecolorgrad)"}]
      ;[brush {:dataKey :name :height 20 :startIndex 0 :endIndex (- (count data) 2)}
       ; [area {:type :monotone :dataKey :measurement :stroke "#8884d8" :fillOpacity 1 :fill "url(#bluecolorgrad)"}]
        ;[area {:type :monotone :dataKey :measurement :stroke "#8884d8" :fillOpacity 1 :fill "url(#bluecolorgrad)"}]
      ; ]
      ]]
    ))

(defn brush-chart []
  (let [data @(subscribe [:results])
        granularity @(subscribe [:granularity])
        this-xaxis-time-renderer (partial xaxis-time-renderer granularity)]
    [responsive-container {:height 30 :width "100%"}
     [composed-chart {:syncId :graphs
                      :data (for [dp data
                                  :let [timestamp (-> dp :timestamp)
                                        measurement (get-in dp [:result :player-count])]
                                  :when (some? timestamp)]
                              {:name (this-xaxis-time-renderer timestamp) :measurement measurement})}
      [brush {:dataKey :name :height 20 :startIndex 0 :endIndex (- (count data) 2)}]]]))

(defn headline-figure [metric &{:keys [formatting] :or {formatting :dec}}]
  (let [final-result @(subscribe [:final-result metric])
        formatter (get formatters formatting)
        formatted-value (formatter final-result)]
    [:p.headline-figure {:class (name metric)} (str formatted-value)]))

(defn filter-option [label value callback checked?]
  [:li
    [:label 
     [:input {:type :checkbox :on-change callback :checked checked?}]
     (str " " label)]])

(defn filter-ordinal-dimension [dimension-name]
  (let [values @(subscribe [:dimension dimension-name])
        current-values @(subscribe [:filter dimension-name])
        on-change (fn [value synthetic-event]
                    (let [active? (-> synthetic-event .-target .-checked)]
                      (dispatch [:slice-ordinal-dimension dimension-name value active?])))]
    [:ul
     (for [value values
           :let [label (if (nil? value) "(unlabeled)" value)
                 on-change-value (partial on-change value)
                 checked? (contains? current-values value)]]
       ^{:key value} [filter-option label value on-change-value checked?])]))

(defn granularity-options []
  (let [granularity @(subscribe [:granularity])
        granularities [[:day "Day"] [:month "Month"] [:year "Year"]]]
    [bs/button-group
     (for [[value label] granularities
           :let [active? (= granularity value)]]
       ^{:key value}
       [bs/button {:class (when active? "btn-primary") :onClick #(rf/dispatch [:set-granularity value])} label])]))

(defn get-event-value [synth-event]
  (-> synth-event .-target .-value))

(defn game-select []
  (let [games @(subscribe [:dimension :game])
        current-game @(subscribe [:filter :game])]
    [:ul.games
     (for [game games
           :let [checked? (= game current-game)]]
       ^{:key game}[:li {:class (if checked? "active" "")} [:label [:input {:checked checked? :onChange #(rf/dispatch [:set-game game]) :type :radio :name :game :value game}] " " game]])]))
     
(defn start-time-select []
  (let [current-start-time @(subscribe [:filter :start-time])]
    [:input {:type :datetime-local :value current-start-time :onChange (fn [evt] (rf/dispatch [:set-start-time (get-event-value evt)]))}]))

(defn end-time-select []
  (let [current-end-time @(subscribe [:filter :end-time])]
    [:input {:type :datetime-local :value current-end-time :onChange (fn [evt] (rf/dispatch [:set-end-time (get-event-value evt)]))}]))

(defn results []
  [:div
   ;[bs/row 
   ; [bs/col {:sm 2}]
   ; [bs/col {:sm 9}
      [:div#brush [brush-chart]];]]
   [bs/row
    [bs/col {:sm 2}
     [:h2 "Player Count"]
     [headline-figure :player-count :formatting :int] [:p {:style {:textAlign :center}} "players"]]
    [bs/col {:sm 9}
     [stats-chart :player-count]]]
   [bs/row
    [bs/col {:sm 2}
     [:h2 "Game Count"]
     [headline-figure :game-count :formatting :int] [:p {:style {:textAlign :center}} "games"]]
    [bs/col {:sm 9}
     [stats-chart :game-count]]]
   [bs/row
    [bs/col {:sm 2}
     [:h2 "Session Count"]
     [headline-figure :session-count :formatting :int] [:p {:style {:textAlign :center}} "sessions"]]
    [bs/col {:sm 9}
     [stats-chart :session-count]]]
   [bs/row
    [bs/col {:sm 2}
     [:h2 "LTJ"]
     [:p "The average judgements made over a players lifetime of play"]
     [headline-figure :LTJ]]
    [bs/col {:sm 9}
     [stats-chart :LTJ]]]
   [bs/row
    [bs/col {:sm 2}
     [:h2 "AjPp"]
     [:p "The average judgements made over a players session"]
     [headline-figure :AjpP]]
    [bs/col {:sm 9}
     [stats-chart :AjpP]]]
   [bs/row
    [bs/col {:sm 2}
     [:h2 "ASL"]
     [:p "Average session length"]
     [headline-figure :ASL :formatting :duration]]
    [bs/col {:sm 9}
     [stats-chart :ASL :formatting :duration]]]
   [bs/row
    [bs/col {:sm 2}
     [:h2 "ALP"]
     [:p "Total play time, averaged over players (possibly across multiple sessions)"]
     [headline-figure :ALP :formatting :duration]]
    [bs/col {:sm 9}
     [stats-chart :ALP :formatting :duration]]]
   [bs/row
    [bs/col {:sm 2}
     [:h2 "Judgement Count"]
     [:p "Total number of judgements provided by all players"]
     [headline-figure :judgement-count :formatting :int]]
    [bs/col {:sm 9}
     [stats-chart :judgement-count]]]
   [bs/row
    [bs/col {:sm 2}
     [:h2 "Thoughput"]
     [:p "Judgements made over time"]
     [headline-figure :throughput]]
    [bs/col {:sm 9}
     [stats-chart :throughput]]]])

(defn home-page []
  (let [data @(subscribe [:results])
        dimensions @(subscribe [:dimensions])
        game-selected (some? @(subscribe [:filter :game]))
        final-results @(subscribe [:final-result])]
    [:div
       [:nav.navbar.navbar-inverse.navbar-fixed-top
        [:div.container-fluid
         [:div.navbar-header
          [:a.navbar-brand {:href "#"} "GWAP Analytics - Dashboard"]]
        [:div.collapse.navbar-collapse
          [:ul.nav.navbar-nav.navbar-right
            [:li {:style {:color :white}}
              
              [:a {:href "https://dali.eecs.qmul.ac.uk/"} "GWAP Analytics is a DALI project"]]]]]]
       [bs/grid {:fluid true}
          [bs/row
           [:div.col-sm-3.sidebar
            [game-select]
            [:hr]
            [:h2 "Granularity"]
            [granularity-options]
            [:h2 "Interval"]
            [:div
             [:label 
              "Start: " [start-time-select]
              ]]
            [:div
             [:label
              "End: " [end-time-select]
              ]]
            (when game-selected
              [:div
               [:h2 "Campaign"]
               [filter-ordinal-dimension :campaign]
               [:h2 "Experiment"]
               [filter-ordinal-dimension :experiment]])]
           [:div.col-sm-9.col-sm-offset-3.main
            (if game-selected [results] [:h2 "Please select a game..."])]]
       ;[:p (str data)]
       ]]))

(defn items-page []
  (fn []
    [:span.main
     [:h1 "The items of dashboard"]
     [:ul (map (fn [item-id]
                 [:li {:name (str "item-" item-id) :key (str "item-" item-id)}
                  [:a {:href (path-for :item {:item-id item-id})} "Item: " item-id]])
               (range 1 60))]]))


(defn item-page []
  (fn []
    (let [routing-data (session/get :route)
          item (get-in routing-data [:route-params :item-id])]
      [:span.main
       [:h1 (str "Item " item " of dashboard")]
       [:p [:a {:href (path-for :items)} "Back to the list of items"]]])))


(defn about-page []
  (fn [] [:span.main
          [:h1 "About dashboard"]]))


;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :about #'about-page
    :items #'items-page
    :item #'item-page))


;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:header
        [:p [:a {:href (path-for :index)} "Home"] " | "
         [:a {:href (path-for :about)} "About dashboard"]]]
       [page]
       [:footer
        [:p "GWAP Analytics is a DALI project"]]])))

;; -------------------------
;; Initialize app

               
(defn mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)
        ))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (rf/dispatch-sync [:initialize])
  ;(rf/dispatch [:update-results {}])
  (rf/dispatch [:update-dimensions {}])
  (rf/dispatch [:result-refresh])
  (mount-root))
