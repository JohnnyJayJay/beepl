(ns beepl.command
  (:require [slash.command :refer [defhandler]]
            [slash.response :as rsp]
            [beepl.state :refer [translation-queries]]
            [beepl.form :refer [form]])
  (:import (java.util.concurrent Executors TimeUnit)))

(def scheduler (Executors/newSingleThreadScheduledExecutor))

(defhandler translate-handler
    ["Translate"]
    {:keys [id]
     {{user-id :id} :user} :member
     {target-id :target_id {{message (keyword target-id)} :messages} :resolved} :data :as interaction}
    _
  (swap! translation-queries assoc id message)
  (.schedule scheduler #(swap! translation-queries dissoc id) 15 TimeUnit/MINUTES)
  (-> {:content (str "<@" user-id ">, please select a source and target language for the translation of the message.")
       :components (form id :source-default "detect" :submit-disabled? true)}
      rsp/channel-message
      rsp/ephemeral)
  #_(rsp/channel-message {:content "hi"}))
