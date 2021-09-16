(ns beepl.form
  (:require [beepl.state :refer [translation-queries config discord-conn]]
            [beepl.deepl :as deepl]
            [slash.response :as rsp]
            [slash.component.structure :as cmp]
            [clojure.string :as str]
            [discljord.messaging :as discord-rest]
            [discljord.cdn :as discord-cdn]
            [discljord.formatting :as discord-fmt]))

(def supported-langs
  (sorted-map
   "RU" {:name "Russian", :emoji "🇷🇺"},
   "IT" {:name "Italian", :emoji "🇮🇹"},
   "FI" {:name "Finnish", :emoji "🇫🇮"},
   "CS" {:name "Czech", :emoji "🇨🇿"},
   "LT" {:name "Lithuanian", :emoji "🇱🇹"},
   "BG" {:name "Bulgarian", :emoji "🇧🇬"},
   "EL" {:name "Greek", :emoji "🇬🇷"},
   "JA" {:name "Japanese", :emoji "🇯🇵"},
   "HU" {:name "Hungarian", :emoji "🇭🇺"},
   "RO" {:name "Romanian", :emoji "🇷🇴"},
   "SV" {:name "Swedish", :emoji "🇸🇪"},
   "EN-US" {:name "English", :emoji "🇺🇸"},
   "FR" {:name "French", :emoji "🇫🇷"},
   "DE" {:name "German", :emoji "🇩🇪"},
   "ET" {:name "Estonian", :emoji "🇪🇪"},
   "DA" {:name "Danish", :emoji "🇩🇰"},
   "NL" {:name "Dutch", :emoji "🇳🇱"},
   "LV" {:name "Latvian", :emoji "🇱🇻"},
   "SK" {:name "Slovak", :emoji "🇸🇰"},
   "ES" {:name "Spanish", :emoji "🇪🇸"},
   "ZH" {:name "Chinese", :emoji "🇨🇳"},
   "SL" {:name "Slovenian", :emoji "🇸🇮"},
   "PT-PT" {:name "Portuguese", :emoji "🇵🇹"},
   "PL" {:name "Polish", :emoji "🇵🇱"}))

(defn lang-options [default]
  (for [[lang-id {:keys [name emoji]}] supported-langs]
    (cmp/select-option name lang-id :emoji {:name emoji} :default (= default lang-id))))

(defn source-options [default]
  (cons (cmp/select-option
         "Detect Language"
         "detect"
         :description "Detect the input language"
         :emoji {:name "💡"}
         :default (= default "detect"))
        (lang-options default)))

(defn submit-button [id disabled]
  (cmp/button :sucess (str id \_ "submit") :label "Translate" :emoji {:name "🌐"} :disabled disabled))

(defn form [id source-default target-default submit-disabled?]
  [(cmp/action-row
    (cmp/select-menu
     (str id \_ "source")
     (source-options source-default)
     :placeholder "Source Language"))
   (cmp/action-row
    (cmp/select-menu
     (str id \_ "target")
     (lang-options target-default)
     :placeholder "Target Language"))
   (cmp/action-row
    (submit-button id submit-disabled?))])

(defn parse-action [action]
  (str/split action #"_" 2))

(defn translation-response [{}])

(defmulti handle-action (fn [id component value query interaction] component))

(defmethod handle-action "submit"
  [id _ _
   {:keys [content source-lang target-lang author] message-id :id}
   {app-id :application_id interact-token :token guild-id :guild_id channel-id :channel_id {:keys [user]} :member}]
  (future
    (let [{[{detected-lang :detected_source_language translated-text :text}] :translations}
          (deepl/translate (:deepl-key config) content source-lang target-lang)
          {source-name :name source-emoji :emoji} (supported-langs (or source-lang detected-lang))
          {target-name :name target-emoji :emoji} (supported-langs target-lang)]
      (discord-rest/create-followup-message!
       discord-conn
       app-id
       interact-token
       :content ""
       :allowed-mentions []
       :components []
       :embeds
       [{:description
         (str translated-text
              \newline
              (discord-fmt/embed-link
               "Original message"
               (str "https://discord.com/channels/" guild-id \/ channel-id \/ message-id)))
         :author {:name (discord-fmt/user-tag author)
                  :icon_url (discord-cdn/effective-user-avatar author)}
         :footer {:text (str "Translated from " source-name " " source-emoji " to " target-name " " target-emoji " - Requested by " (discord-fmt/user-tag user))}}])))
  (swap! translation-queries dissoc id)
  (rsp/update-message {:content "Translating, please wait...", :components []}))

(defmethod handle-action "target"
  [id _ value {:keys [source-lang]} _]
  (swap! translation-queries assoc-in [id :target-lang] value)
  (rsp/update-message {:components (form id (or source-lang "detect") value false)}))

(defmethod handle-action "source"
  [id _ value _ _]
  (swap! translation-queries assoc-in [id :source-lang] (if (= value "detect") nil value))
  rsp/deferred-update-message)

(defn form-handler [{{action :custom_id [value] :values} :data :as interaction}]
  (let [[id component] (parse-action action)]
    (if-let [query (@translation-queries id)]
      (handle-action id component value query interaction)
      (rsp/update-message {:components [] :content "Sorry, too much time has passed since you clicked translate. Please try again."}))))
