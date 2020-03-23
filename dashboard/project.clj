(defproject dashboard "0.1.3-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "0.2.7"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/core.async  "0.4.474"]
                 [com.fasterxml.jackson.core/jackson-databind "2.10.0"]
                 [com.fasterxml.jackson.core/jackson-core "2.10.0"]
                 [ring-server "0.5.0"]
                 [ring-cors "0.1.13"]
                 [reagent "0.9.0-rc3"]
                 [reagent "0.8.1"]
                 [reagent-utils "0.3.3"]
                 ;[com.google.guava/guava "21.0"]
                 [com.google.guava/guava "28.0-jre"]
                 [clj-http "3.4.1"]
                 [medley "1.2.0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [ring "1.8.0"]
                 [ring/ring-defaults "0.3.2"]
                 [metosin/ring-http-response "0.9.1"]
                 [swiss-arrows "1.0.0"]
                 [prismatic/schema "1.1.3"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [curator "0.0.6"]
                 ;[y42/clj-druid "0.2.18"]
                 ;[y42/clj-druid "0.2.18-finalizingfield-fix"]
                 [com.github.chrism-qmul/clj-druid "0.2.18-finalizingfield-fix"]
                 ;[cljsjs/react-chartjs-2 "2.8.0"]
                 [cljsjs/recharts "1.6.2-0"]
                 [cljs-http "0.1.11"]
                 [clj-time "0.15.2"]
                 [hiccup "1.0.5"]
                 [fundingcircle/jackdaw "0.7.0"]
                 [yogthos/config "1.1.6"]
                 [re-frame "0.10.9"]
                 [digest "1.4.9"]
                 [org.clojure/clojurescript "1.10.597"
                  :scope "provided"]
                 [metosin/reitit "0.3.7"]
                 [pez/clerk "1.0.0"]
                 [cljsjs/react-bootstrap "0.31.5-0"]
                 [venantius/accountant "0.2.5"
                  :exclusions [org.clojure/tools.reader]]]

  :plugins [[lein-environ "1.1.0"]
            [deraen/lein-less4j "0.6.2"]
            [lein-cljsbuild "1.1.7"]
            [lein-asset-minifier "0.4.6"
             :exclusions [org.clojure/clojure]]]

  :ring {:handler dashboard.handler/app
         :uberwar-name "dashboard.war"}

  :repositories {"local" "file:maven"}

  :min-lein-version "2.5.0"
  ;:uberjar-name "dashboard.jar"
  :main dashboard.server
  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :resource-paths ["resources" "target/cljsbuild"] ;"lib/clj-druid-0.2.18-finalizingfield-fix.jar"

  :minify-assets
  [[:css {:source "resources/public/css/site.css"
          :target "resources/public/css/site.min.css"}]]

  :cljsbuild
  {:builds {:min
            {:source-paths ["src/cljs" "src/cljc" "env/prod/cljs"]
             :compiler
             {:output-to        "target/cljsbuild/public/js/app.js"
              :output-dir       "target/cljsbuild/public/js"
              :source-map       "target/cljsbuild/public/js/app.js.map"
              :optimizations :advanced
              :infer-externs true
              :pretty-print  false}}
            :app
            {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
             :figwheel {:on-jsload "dashboard.core/mount-root"}
             :compiler
             {:main "dashboard.dev"
              :asset-path "/js/out"
              :output-to "target/cljsbuild/public/js/app.js"
              :output-dir "target/cljsbuild/public/js/out"
              :source-map true
              :optimizations :none
              :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}
              :preloads [day8.re-frame-10x.preload devtools.preload]
              :pretty-print  true}}
              


            }
   }

  :figwheel
  {:http-server-root "public"
   :server-port 3449
   :nrepl-port 7002
   :nrepl-middleware [cider.piggieback/wrap-cljs-repl
                      ]
   :css-dirs ["resources/public/css"]
   :ring-handler dashboard.handler/app}

  :less {:source-paths ["src/less"]
         :target-path "resources/public/css"}


  :profiles {:dev {:repl-options {:init-ns dashboard.repl}
                   :dependencies [[cider/piggieback "0.4.2"]
                                  [binaryage/devtools "0.9.11"]
                                  [ring/ring-mock "0.4.0"]
                                  [ring/ring-devel "1.8.0"]
                                  [prone "2019-07-08"]
                                  [figwheel-sidecar "0.5.19"]
                                  [nrepl "0.6.0"]
                                  [day8.re-frame/re-frame-10x "0.4.3"]
                                  [pjstadig/humane-test-output "0.10.0"]
                                  
                                  ;; To silence warnings from less4clj dependecies about missing logger implementation
                                  [org.slf4j/slf4j-nop "1.7.25"]
 ]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.5.19"]
]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :env {:dev true}}

             :uberjar {:hooks [minify-assets.plugin/hooks]
                       :source-paths ["env/prod/clj"]
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
                       :env {:production true}
                       :aot :all
                       :omit-source true}})
