(ns beepl.state
  (:require [mount.core :refer [defstate]]
            [clojure.edn :as edn]
            [discljord.messaging :as discord-rest]))

(defstate translation-queries
  :start (atom {}))

(defstate config
  :start (edn/read-string (slurp "config/config.edn")))

(defstate discord-conn
  :start (discord-rest/start-connection! (:bot-token config))
  :stop (discord-rest/stop-connection! discord-conn))

(defonce translation-queries
  (atom {}))
