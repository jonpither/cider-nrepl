(ns cider.nrepl.middleware.nses
  (:require [cider.nrepl.middleware.util.cljs :as cljs]
            [cider.nrepl.middleware.util.misc :as u]
            [clojure.tools.nrepl.transport :as transport]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.middleware :refer [set-descriptor! ]]))

(def recent-nses (atom '()))

(defn- add-to-front-of-list [recent-nses ns]
  (cons ns (remove #{ns} recent-nses)))

(defn nses-reply
  [{:keys [transport] :as msg}]
  (transport/send transport (response-for msg :value (u/transform-value (clojure.string/join " " @recent-nses)))))

(defn wrap-info
  "Middleware that looks up info for a symbol within the context of a particular namespace."
  [handler]
  (fn [{:keys [op ns] :as msg}]
    (if (= "nses-fetch" op)
      (#'nses-reply msg)
      (do
        (when ns
          (swap! recent-nses add-to-front-of-list ns))
        (handler msg)))))

(set-descriptor!
 #'wrap-info
 (cljs/maybe-piggieback
  {:handles
   {"info"
    {:doc "Return a list of recently used namespaces."
     :returns {"status" "done"}}}}))
