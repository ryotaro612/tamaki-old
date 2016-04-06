(ns tamaki-core.server
  (:require [tamaki-core.config :refer [read-config]]
            [tamaki-core.file :as tfile]
            [tamaki.file.copy :as cpy]
            [tamaki.template.page :as page]
            [tamaki.sitemap.sitemap :as sitemap]
            [compojure.core :refer [GET defroutes]]
            [clojure.string :as string]
            [compojure.route :as route]
            [clojure.java.io :as io]
             [ring.util.response :refer [redirect]]))


(defn tcompile []
  (tfile/clean-dest)
  (cpy/copy)
  (page/compile-mds)

  (spit (io/file (io/file (-> (read-config) :dest)) "sitemap.xml")
         (sitemap/create-sitemap (:page-dir (read-config)) (:post-dir (read-config)) (:url (read-config))))

  (let [url (string/replace (:url (read-config)) #"([^/])$" "$1/")]
    (spit (io/file (io/file (-> (read-config) :dest)) "robots.txt")
          (str "User-agent: *\nSitemap: " url "sitemap.xml\nDisallow:" )))

  (page/gen-paginate-page (-> (read-config) :post-dir))
  (page/compile-postmds (-> (read-config) :post-dir)))

(defn init [] (tcompile))

(defroutes handler
           (GET ":prefix{.*}/" [prefix] (redirect (str prefix "/index.html")))
           (route/resources "/" {:root (string/replace (:dest (read-config)) #"^resources/" "")})
  (route/not-found "Page not found"))
