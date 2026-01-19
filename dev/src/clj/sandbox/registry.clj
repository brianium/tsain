(ns sandbox.registry
  (:require [ascolais.sandestin :as s]
            [ascolais.sfere :as sfere]
            [ascolais.twk :as twk]
            [sandbox.state :as state]
            [sandbox.views :as views]))

(defn- broadcast-view!
  "Broadcast current view state to all connected clients."
  [dispatch state-atom]
  (dispatch {} {}
            [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
              [::twk/patch-elements (views/render-view @state-atom)]]]))

(defn registry
  "Create sandbox registry with state atom."
  [state-atom]
  {::s/effects
   {::preview
    {::s/description "Set preview content and broadcast to clients"
     ::s/schema [:tuple [:= ::preview] :any]
     ::s/handler
     (fn [{:keys [dispatch]} _system hiccup]
       (swap! state-atom assoc
              :preview {:hiccup hiccup}
              :view {:type :preview})
       (broadcast-view! dispatch state-atom))}

    ::preview-append
    {::s/description "Append content to preview"
     ::s/schema [:tuple [:= ::preview-append] :any]
     ::s/handler
     (fn [{:keys [dispatch]} _system hiccup]
       (swap! state-atom update-in [:preview :hiccup]
              (fn [existing]
                (if existing
                  [:div existing hiccup]
                  hiccup)))
       (swap! state-atom assoc :view {:type :preview})
       (broadcast-view! dispatch state-atom))}

    ::preview-clear
    {::s/description "Clear preview content"
     ::s/schema [:tuple [:= ::preview-clear]]
     ::s/handler
     (fn [{:keys [dispatch]} _system]
       (swap! state-atom assoc
              :preview {:hiccup nil}
              :view {:type :preview})
       (broadcast-view! dispatch state-atom))}

    ::commit
    {::s/description "Commit current preview to library"
     ::s/schema [:tuple [:= ::commit] :keyword [:maybe :string]]
     ::s/handler
     (fn [{:keys [dispatch]} _system component-name description]
       (let [{:keys [preview]} @state-atom
             hiccup (:hiccup preview)]
         (when hiccup
           (swap! state-atom assoc-in [:library component-name]
                  {:hiccup hiccup
                   :description (or description "")
                   :created-at (java.util.Date.)})
           (state/save-library! (:library @state-atom))
           (broadcast-view! dispatch state-atom))))}

    ::uncommit
    {::s/description "Remove component from library"
     ::s/schema [:tuple [:= ::uncommit] :keyword]
     ::s/handler
     (fn [{:keys [dispatch]} _system component-name]
       (swap! state-atom update :library dissoc component-name)
       (state/save-library! (:library @state-atom))
       (broadcast-view! dispatch state-atom))}

    ::show
    {::s/description "Show a single component"
     ::s/schema [:tuple [:= ::show] :keyword]
     ::s/handler
     (fn [{:keys [dispatch]} _system component-name]
       (swap! state-atom assoc :view {:type :component :name component-name})
       (broadcast-view! dispatch state-atom))}

    ::show-gallery
    {::s/description "Show component gallery"
     ::s/schema [:tuple [:= ::show-gallery]]
     ::s/handler
     (fn [{:keys [dispatch]} _system]
       (swap! state-atom assoc :view {:type :gallery})
       (broadcast-view! dispatch state-atom))}

    ::show-preview
    {::s/description "Switch to preview view"
     ::s/schema [:tuple [:= ::show-preview]]
     ::s/handler
     (fn [{:keys [dispatch]} _system]
       (swap! state-atom assoc :view {:type :preview})
       (broadcast-view! dispatch state-atom))}

    ::sync-view
    {::s/description "Sync a client to current view state"
     ::s/schema [:tuple [:= ::sync-view]]
     ::s/handler
     (fn [_ctx _system]
       ;; Returns effects to be sent to the connecting client
       [[::twk/patch-elements (views/render-view @state-atom)]])}}})
