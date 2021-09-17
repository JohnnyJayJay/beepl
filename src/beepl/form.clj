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
   "EN" {:name "English", :emoji "🇺🇸"},
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
   "PT" {:name "Portuguese", :emoji "🇵🇹"},
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

(defn toggle-public-button [id public?]
  (cmp/button
   (if public? :success :danger)
   (str id \_ "toggle-public")
   :label (str (if public? "Everybody" "Only you") " will see the translation")
   :emoji {:name (if public? "🔊" "🔇")}))

(defn submit-button [id disabled]
  (cmp/button :primary (str id \_ "submit") :label "Translate" :emoji {:name "💬"} :disabled disabled))

(defn form [id & {:keys [source-default target-default submit-disabled? public?]}]
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
    (toggle-public-button id public?))
   (cmp/action-row
    (submit-button id submit-disabled?))])

(defn parse-action [action]
  (str/split action #"_" 2))

(defn submit-query
  [id
   {:keys [content source-lang target-lang author public?] message-id :id :as _query}
   {app-id :application_id interact-token :token guild-id :guild_id channel-id :channel_id {:keys [user]} :member :as _interaction}]
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
       :flags (if public? 0 64)
       :components [(cmp/action-row (cmp/link-button (str "https://discord.com/channels/" guild-id \/ channel-id \/ message-id) :label "Original message"))]
       :embeds
       [{:description translated-text
         :author {:name (discord-fmt/user-tag author)
                  :icon_url (discord-cdn/effective-user-avatar author)}
         :footer {:text (str "Translated from " source-name " " source-emoji " to " target-name " " target-emoji " - Requested by " (discord-fmt/user-tag user))}}])))
  (swap! translation-queries dissoc id)
  (rsp/update-message {:content "Translating, please wait...", :components []}))

(defmulti update-query (fn [action query value] action))

(defmethod update-query "target"
  [_ query value]
  (assoc query :target-lang value))

(defmethod update-query "source"
  [_ query value]
  (assoc query :source-lang (if (= value "detect") nil value)))

(defmethod update-query "toggle-public"
  [_ query _]
  (update query :public? not))

(defn query->form [id {:keys [source-lang target-lang public?] :as _query}]
  (form id
        :source-default (or source-lang "detect")
        :target-default target-lang
        :submit-disabled? (nil? target-lang)
        :public? public?))

(defn form-handler [{{action :custom_id [value] :values} :data :as interaction}]
  (let [[id component] (parse-action action)]
    (if-let [query (@translation-queries id)]
      (if (= component "submit")
        (submit-query id query interaction)
        (let [updated (update-query component query value)]
          (swap! translation-queries assoc id updated)
          (rsp/update-message {:components (query->form id updated)})))
      (rsp/update-message {:components [] :content "Sorry, too much time has passed since you clicked translate. Please try again."}))))
