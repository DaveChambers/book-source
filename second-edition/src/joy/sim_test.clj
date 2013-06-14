(ns joy.sim-test
  (:require [joy.event-sourcing :as es]
            [joy.generators :refer (rand-map)]
            [clojure.set :as sql]))


(def db (ref #{{:player "Nick", :ability 32}
               {:player "Matt", :ability 26}
               {:player "Ryan", :ability 19}}))

(defn update-stats [db event]
  (let [player (first (sql/select #(= (:player event)
                                      (:player %))
                                  db))
        less-db  (sql/difference db #{player})]
    (conj less-db (merge player (es/effect player event)))))

(comment

  (sql/select #(= "Nick" (:player %)) @db)

  ;;=> #{{:ability 32, :player "Nick"}}

  (update-stats @db {:player "Nick", :result :hit})

)

(defn rand-event [max player]
  (rand-map 1
            #(-> :result)
            #(if (< (rand-int max) (:ability player))
               :hit
               :out)))

(defn rand-events [total max player]
  (take total
        (repeatedly #(assoc (rand-event max player) :player (:player player)))))

(comment

  (rand-events 10 100 {:player "Nick", :ability 32})
  
  (reduce
   #(+ %1 (if (= :hit (:result %2)) 1 0))
   0
   (take 100 (repeatedly #(rand-event 100 {:player "Nick", :ability 32}))))

)

(def agent-for-player
  (memoize
   (fn [player-name]
     (agent nil))))

(agent-for-player "Nick")

(defn feed [db event]
  (send (agent-for-player (:player event))
        (fn [state]
          (dosync (alter db update-stats event))
          state)))

(defn feed-all [db events]
  (doseq [event events]
    (feed db event))
  db)


(comment

  (feed-all (ref @db) (rand-events 10 100 {:player "Nick", :ability 32}))

)

(defn simulate [db player events]
  (let []))

