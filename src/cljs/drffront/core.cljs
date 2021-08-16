(ns drffront.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [cljs-http.client :as http]
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [accountant.core :as accountant]))

;; -------------------------
;; Routes

(def service-routes
  {:controls "http://localhost:8000/api/control/"
   :riders "http://localhost:8000/api/rider/"
   :snippets "http://localhost:8000/api/snippets/"})

(def router
  (reitit/router
   [["/" :index]
    ["/items"
     ["" :items]
     ["/:item-id" :item]]
    ["/about" :about]
    ["/controls" :controls]
    ["/riders" :riders]
    ]))


(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;; -------------------------
;; Page components

(def alert-style (str  "bg-green-100 border border-red-400 text-red-700 px-4 py-3 rounded relative my-5"
                       " bg-gradient-to-b from-red-200 to-blue-300"))
(def card-style "max-w-sm mx-auto flex p-6 bg-white rounded-lg shadow-xl")

(defn home-page []
  (fn []
    [:span.main
      [:h1 "Welcome to drffront"]
     [:div.container.grid.grid-cols-2.gap-4.bg-yellow-700

      ;; [:ul
      ;;  [:li [:a {:href (path-for :items)} "Items of drffront"]]
      ;;  [:li [:a {:href "/broken/link"} "Broken link"]]]

      [:div.bg-yellow-100.border.border-red-400
       [:button.bg-blue-500.text-white.font-bold.p-4.rounded.hover:bg-blue-200 "More"]]

      [:div.bg-green-100.border.border-red-400
       [:div {:class alert-style
              :role "alert"}
        [:strong.font-bold "Alert!"]
        [:span.block.sm:inline " Please update your password!"]]]

       ;; [:div {:class card-style}]
      [:div.bg-red-100 "Hello, world"]
      [:div.bg-red-200 "Goodbye, world"]
      ]

     ;; [:div.bg-green-300.border-green-600.border-b.p-4.m-4.rounded.hover:bg-red-200  "Hello, world."]

     ]
    ))



(defn items-page []
  (fn []
    [:span.main
     [:h1 "The items of drffront"]
     [:ul (map (fn [item-id]
                 [:li {:name (str "item-" item-id) :key (str "item-" item-id)}
                  [:a {:href (path-for :item {:item-id item-id})} "Item: " item-id]])
               (range 1 60))]]))


(defn item-page []
  (fn []
    (let [routing-data (session/get :route)
          item (get-in routing-data [:route-params :item-id])]
      [:span.main
       [:h1 (str "Item " item " of drffront")]
       [:p [:a {:href (path-for :items)} "Back to the list of items"]]])))


(defn about-page []
  (fn [] [:span.main
          [:h1 "About drffront"]]))

(defn loaddata [atm url]
  (go (let [response (<! (http/get url {:with-credentials? false}))]
        (reset! atm response)
        ;;(js/console.log "Response retrieved" response)
        )))

(defn postdata []
  (go (let [response (<! (http/post (:snippets service-routes)
                                    {:with-credentials? false
                                     :json-params {:title "Test2 from cljs" :code "(prn \"testing\""}}))]
        ;;(js/console.log "Post channel returns:" response (:body response))
        )))

;; (postdata)

(defn controls-page []
  (let [response (atom {:loading true})]
    (loaddata response (:controls service-routes))
    (fn []
      [:span.main
       [:h1 "Controls Page"]
       (cond
         (:loading @response)
         [:div "Loading control data..."]

         (not (:success @response))
         [:div "Loading control data failed."]

         true
         [:table {:style {:border "1px solid red"}}
          [:tbody
           (for [control (get-in @response [:body :results])]
             ^{:key (:id control)}
             [:tr
              [:td (:id control)]
              [:td (:name control)]
              [:td (:distance control)]
              ])]])])))

(defn riders-page []
  (let [response (atom {:loading true})]
    (loaddata response (:riders service-routes))
    (fn []
      [:span.main
       [:h1 "Riders Page"]
       (cond
         (:loading @response)
         [:div "Loading rider data..."]

         (not (:success @response))
         [:div "Loading rider data failed."]

         true
         [:table {:style {:border "1px solid orange"}}
          [:tbody
           (for [rider (get-in @response [:body :results])]
             ^{:key (:id rider)}
             [:tr
              [:td (:id rider)]
              [:td (:first_name rider)]
              [:td (:last_name rider)]
              [:td (:country rider)] [:br]])]])])))

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :about #'about-page
    :items #'items-page
    :item #'item-page
    :controls #'controls-page
    :riders #'riders-page
    ))

;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:header
        [:p [:a {:href (path-for :index)} "Home"] " | "
         [:a {:href (path-for :controls)} "Controls"] " | "
         [:a {:href (path-for :riders)} "Riders"] " | "
         [:a {:href (path-for :about)} "About drffront"]]]
       [page]
       [:footer
        [:p "drffront was generated by the "
         [:a {:href "https://github.com/reagent-project/reagent-template"} "Reagent Template"] "."]]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)
        ))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))
