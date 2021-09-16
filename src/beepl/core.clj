(ns beepl.core
  (:gen-class)
  (:require [org.httpkit.server :as http-server]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.util.response :refer [response]]
            [ring-discord-auth.ring :refer [wrap-authenticate]]
            [slash.core :as slash]
            [slash.webhook :as slash-wh]
            [beepl.command :refer [translate-handler]]
            [beepl.form :refer [form-handler]]
            [beepl.state :refer [config]]
            [ring-debug-logging.core :refer [wrap-with-logger]]
            [mount.core :as mount]))

(def handler #(response (slash/route-interaction (assoc slash-wh/webhook-defaults :application-command translate-handler :message-component form-handler) (:body %))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Starting server...")
  (mount/start)
  (http-server/run-server
   (-> handler
       wrap-json-response
       (wrap-json-body {:keywords? true})
       (wrap-authenticate (:public-key config))
       wrap-with-logger)))
