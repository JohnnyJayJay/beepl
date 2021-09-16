(defproject beepl "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [ring/ring-core "1.9.4"]
                 [ring/ring-json "0.5.1"]
                 [http-kit "2.5.3"]
                 [com.github.johnnyjayjay/ring-discord-auth "1.0.0"]
                 [com.github.johnnyjayjay/slash "0.2.0-SNAPSHOT"]
                 [bananaoomarang/ring-debug-logging "1.1.0"]
                 [org.suskalo/discljord "1.3.0-RC1"]
                 [mount "0.1.16"]]
  :main ^:skip-aot beepl.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
