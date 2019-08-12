(ns clograms.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [clograms.core :refer [re-index-all db-edn]]
            [clojure.string :as str]
            [ring.middleware.cors :refer [wrap-cors]]))

(defn file-content [path line]
  (let [content (if line
                  (->> (slurp path)
                       (str/split-lines)
                       (map-indexed (fn [lnumber l]
                                      (if (= (inc lnumber) line)
                                        (str "<b id=\"line-tag\" style=\"background-color:yellow\">" l "</b>\n")
                                        (str l "\n"))))
                       (apply str))
                  (slurp path))]
   (str "<p style=\"white-space: pre-wrap\">" content "</p>")))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/open-file" [path line :as req]
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (file-content path (Integer/parseInt line))})
  (GET "/db" []
       {:status 200
        :headers {"Content-Type" "application/edn"}
        :body (db-edn)})
  (resources "/"))

(def handler (-> #'routes
                 (wrap-cors :access-control-allow-origin [#"http://localhost:9500"]
                            :access-control-allow-methods [:get :put :post :delete])
                 wrap-keyword-params
                 wrap-params))
