(ns beepl.deepl
  (:require [org.httpkit.client :as http-client]
            [cheshire.core :as json]))

(def deepl-url "https://api-free.deepl.com/v2/translate")

(defn translate [api-key text source-lang target-lang]
  (-> @(http-client/post
        deepl-url
        {:headers {"Authorization" (str "DeepL-Auth-Key " api-key)}
         :query-params
         (cond-> {"text" text, "target_lang" target-lang}
           source-lang (assoc "source_lang" source-lang))})
      :body
      (json/parse-string keyword)))
