(defproject dv "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[bouncer "1.0.0"]
                 [ch.qos.logback/logback-classic "1.1.8"]
                 [cljs-ajax "0.5.8"]
                 [compojure "1.6.0-beta2"]
                 [cprop "0.1.9"]
                 [luminus-jetty "0.1.4"]
                 [luminus-nrepl "0.1.4"]
                 [luminus/ring-ttl-session "0.3.1"]
                 [markdown-clj "0.9.91"]
                 [metosin/ring-http-response "0.8.0"]
                 [mount "0.1.11"]
                 [org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.456" :scope "provided"]
                 [org.clojure/tools.cli "0.3.5"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [org.postgresql/postgresql "9.4.1212"]
                 [com.carouselapps/to-jdbc-uri "0.5.0"]
                 [org.clojure/core.async "0.2.395" :exclusions [com.google.guava/guava]]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.webjars.bower/tether "1.3.7"]
                 [org.webjars/bootstrap "4.0.0-alpha.5"]
                 [org.webjars/font-awesome "4.7.0"]
                 [re-frame "0.9.1"]
                 [day8.re-frame/http-fx "0.1.3"]
                 [reagent "0.6.0"]
                 [reagent-utils "0.2.0"]
                 [ring-middleware-format "0.7.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [ring-cors "0.1.8"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [graphql-clj "0.1.20" :exclusions [org.clojure/clojure]]
                 [secretary "1.2.3"]
                 [selmer "1.10.3"]
                 [junit/junit "4.12"]
                 [javax.mail/mail "1.4.7"]
                 [javax.mail/javax.mail-api "1.5.6"]
                 [com.google.guava/guava "20.0"]]

  :min-lein-version "2.0.0"

  :jvm-opts ["-server" "-Dconf=.lein-env"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main dv.core

  :plugins [[lein-cprop "1.0.1"]
            [lein-cljsbuild "1.1.4"]]
  :clean-targets ^{:protect false}
  [:target-path [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]
  :figwheel
  {:http-server-root "public"
   :nrepl-port 7002
   :css-dirs ["resources/public/css"]
   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}


  :profiles
  {:uberjar {:omit-source true
             :prep-tasks [["javac"] ["compile"] ["cljsbuild" "once" "min"]]
             :cljsbuild
             {:builds
              {:min
               {:source-paths ["src/cljc" "src/cljs" "env/prod/cljs"]
                :compiler
                {:output-to "target/cljsbuild/public/js/app.js"
                 :optimizations :advanced
                 :pretty-print false
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}
                 :externs ["react/externs/react.js"]}}}}


             :aot :all
             :uberjar-name "dv.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:dependencies [[prone "1.1.4"]
                                 [ring/ring-mock "0.3.0"]
                                 [ring/ring-devel "1.5.0"]
                                 [pjstadig/humane-test-output "0.8.1"]
                                 [binaryage/devtools "0.8.3"]
                                 [com.cemerick/piggieback "0.2.2-SNAPSHOT"]
                                 [doo "0.1.7"]
                                 [figwheel-sidecar "0.5.8"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.18.1"]
                                 [lein-doo "0.1.7"]
                                 [lein-figwheel "0.5.8"]
                                 [org.clojure/clojurescript "1.9.456"]]
                  :cljsbuild
                  {:builds
                   {:app
                    {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                     :compiler
                     {:main "dv.app"
                      :asset-path "/js/out"
                      :output-to "target/cljsbuild/public/js/app.js"
                      :output-dir "target/cljsbuild/public/js/out"
                      :source-map true
                      :optimizations :none
                      :pretty-print true}}}}



                  :doo {:build "test"}
                  :source-paths ["env/dev/clj" "test/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:resource-paths ["env/test/resources"]
                  :cljsbuild
                  {:builds
                   {:test
                    {:source-paths ["src/cljc" "src/cljs" "test/cljs"]
                     :compiler
                     {:output-to "target/test.js"
                      :main "dv.doo-runner"
                      :optimizations :whitespace
                      :pretty-print true}}}}}


   :profiles/dev {}
   :profiles/test {}
   :dontcare nil})
