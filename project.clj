(defproject butler "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1934"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
;                 [org.bodil/cljs-noderepl "0.1.10"]
                 ]
  :plugins [[lein-cljsbuild "0.3.2"]]
;  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :cljsbuild
  {:builds
   [{:source-paths ["src"]
     :compiler {:output-to "resources/public/js/main.js"
                :target :nodejs
                :optimizations :simple
                :pretty-print true }}]})
